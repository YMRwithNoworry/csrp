const fs = require("node:fs");
const path = require("node:path");

const sourcePath = path.join(__dirname, "..", "src", "main", "java", "alku", "csrp", "client",
        "CreditsTitleScreenEvents.java");
const source = fs.readFileSync(sourcePath, "utf8");
const failures = [];

function expect(pattern, message) {
    if (!pattern.test(source)) failures.push(message);
}

expect(/Map<Screen, CreditsState>/, "credits UI state is not tracked per screen");
expect(/ScreenEvent\.Render\.Post/, "mouse movement is not observed from title screen rendering");
expect(/ScreenEvent\.MouseButtonPressed\.Pre/, "collapsed credits click is not handled");
expect(/state\.mouseMoved\(event\.getMouseX\(\), event\.getMouseY\(\)\)/,
        "mouse coordinates do not drive automatic collapse");
expect(/state\.isCollapsed\(\)[\s\S]*state\.isMouseOver/, "only the collapsed panel should expand on click");
expect(/private static final int COLLAPSED_WIDTH = 42;/, "collapsed width is not stable");
expect(/\.left\(collapsed \? 0 : 10\)/, "collapsed credits are not docked to the screen edge");
expect(/collapsedHeading\.setDisplay\(collapsed\)/, "collapsed heading visibility is not updated");
expect(/details\.setDisplay\(!collapsed\)/, "full credits visibility is not updated");
expect(/label\("鸣谢"/, "collapsed credits label is missing");
expect(/label\("感谢名单"/, "expanded credits heading is missing");

if (failures.length) {
    console.error("Title credits verification failed:");
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}

console.log("Title credits verification passed.");
