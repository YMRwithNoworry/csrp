const fs = require("fs");
const path = require("path");

const animationRoot = path.resolve("src/main/resources/assets/csrp/animations");
const checkOnly = process.argv.includes("--check");
const requestedPaths = process.argv.slice(2).filter((argument) => argument !== "--check");

function hasSingleOuterPair(expression) {
  if (!expression.startsWith("(") || !expression.endsWith(")")) return false;

  let depth = 0;
  for (let index = 0; index < expression.length; index += 1) {
    const character = expression[index];
    if (character === "(") depth += 1;
    if (character === ")") depth -= 1;
    if (depth === 0 && index < expression.length - 1) return false;
  }
  return depth === 0;
}

function stripOuterPairs(expression) {
  let stripped = expression.trim();
  while (hasSingleOuterPair(stripped)) stripped = stripped.slice(1, -1).trim();
  return stripped;
}

function findTopLevelComparison(expression) {
  let depth = 0;
  for (let index = 0; index < expression.length; index += 1) {
    const character = expression[index];
    if (character === "(") {
      depth += 1;
      continue;
    }
    if (character === ")") {
      depth -= 1;
      continue;
    }
    if (depth !== 0) continue;

    const pair = expression.slice(index, index + 2);
    if (pair === ">=" || pair === "<=") {
      return {index, operator: pair};
    }
    if (character === ">" || character === "<") {
      return {index, operator: character};
    }
  }
  return null;
}

function comparisonGate(condition) {
  const normalized = stripOuterPairs(condition);
  const comparison = findTopLevelComparison(normalized);
  if (!comparison) return null;

  const left = normalized.slice(0, comparison.index).trim();
  const right = normalized.slice(comparison.index + comparison.operator.length).trim();
  if (!left || !right) throw new Error(`Incomplete ternary condition: ${condition}`);

  return switchGate(comparison.operator, left, right);
}

function switchGate(operator, left, right) {
  switch (operator) {
    case ">":
      return `math.ceil(math.clamp((${left})-(${right}),0,1))`;
    case "<":
      return `math.ceil(math.clamp((${right})-(${left}),0,1))`;
    case ">=":
      return `(1-math.ceil(math.clamp((${right})-(${left}),0,1)))`;
    case "<=":
      return `(1-math.ceil(math.clamp((${left})-(${right}),0,1)))`;
    default:
      throw new Error(`Unsupported comparison operator: ${operator}`);
  }
}

function findTopLevelTernary(expression) {
  let depth = 0;
  let question = -1;
  let nestedTernaries = 0;

  for (let index = 0; index < expression.length; index += 1) {
    const character = expression[index];
    if (character === "(") {
      depth += 1;
      continue;
    }
    if (character === ")") {
      depth -= 1;
      continue;
    }
    if (depth !== 0) continue;

    if (character === "?") {
      if (question < 0) question = index;
      else nestedTernaries += 1;
    } else if (character === ":" && question >= 0) {
      if (nestedTernaries > 0) nestedTernaries -= 1;
      else return {question, colon: index};
    }
  }
  return null;
}

function rewriteExpression(expression) {
  let rebuilt = "";
  for (let index = 0; index < expression.length;) {
    if (expression[index] !== "(") {
      rebuilt += expression[index];
      index += 1;
      continue;
    }

    let depth = 1;
    let end = index + 1;
    while (end < expression.length && depth > 0) {
      if (expression[end] === "(") depth += 1;
      if (expression[end] === ")") depth -= 1;
      end += 1;
    }
    if (depth !== 0) throw new Error(`Unbalanced expression: ${expression}`);

    rebuilt += `(${rewriteExpression(expression.slice(index + 1, end - 1))})`;
    index = end;
  }

  const ternary = findTopLevelTernary(rebuilt);
  if (!ternary) return rebuilt;

  const condition = rebuilt.slice(0, ternary.question);
  const whenTrue = rewriteExpression(rebuilt.slice(ternary.question + 1, ternary.colon));
  const whenFalse = rewriteExpression(rebuilt.slice(ternary.colon + 1));
  const gate = comparisonGate(condition);
  if (!gate) return `${condition}?${whenTrue}:${whenFalse}`;
  return `((${whenFalse})+((${gate})*((${whenTrue})-(${whenFalse}))))`;
}

function rewriteValue(value) {
  if (typeof value === "string") return rewriteExpression(value);
  if (Array.isArray(value)) return value.map(rewriteValue);
  if (value && typeof value === "object") {
    return Object.fromEntries(Object.entries(value).map(([key, child]) => [key, rewriteValue(child)]));
  }
  return value;
}

function serializeLikeSource(source, value) {
  const content = source.replace(/\r?\n$/u, "");
  const firstNewline = content.indexOf("\n");
  if (firstNewline < 0) return `${JSON.stringify(value)}\n`;
  const indentation = content.slice(firstNewline + 1).match(/^[ \t]+(?=\S)/u)?.[0] ?? "  ";
  return `${JSON.stringify(value, null, indentation)}\n`;
}

function animationPaths() {
  if (requestedPaths.length > 0) return requestedPaths.map((entry) => path.resolve(entry));
  return fs.readdirSync(animationRoot)
    .filter((entry) => entry.endsWith(".animation.json"))
    .sort()
    .map((entry) => path.join(animationRoot, entry));
}

function run() {
  let changedFiles = 0;
  for (const animationPath of animationPaths()) {
    const source = fs.readFileSync(animationPath, "utf8");
    const parsed = JSON.parse(source);
    const rewrittenValue = rewriteValue(parsed);
    if (JSON.stringify(parsed) === JSON.stringify(rewrittenValue)) continue;
    const rewritten = serializeLikeSource(source, rewrittenValue);

    changedFiles += 1;
    if (!checkOnly) fs.writeFileSync(animationPath, rewritten);
  }
  if (checkOnly && changedFiles > 0) {
    throw new Error(`${changedFiles} animation files still require comparison ternary rewriting`);
  }

  console.log(checkOnly
    ? "Animation comparison ternaries are GeckoLib-compatible."
    : `Rewrote comparison ternaries in ${changedFiles} animation files.`);
}

if (require.main === module) run();

module.exports = {rewriteValue};
