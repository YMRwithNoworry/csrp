const fs = require("node:fs");
const path = require("node:path");

// The Book of Vengeance grapple is a faithful port of 1.10.9 item/VengeanceGrappleHandler plus the
// knockback burst from item/ItemBookOfVengeance.  Every number below was read off the original, and the
// per-stage specification is in docs/gap/VENGEANCE_GRAPPLE_REPORT.md — this script exists so a future
// edit cannot quietly drift away from it again (the previous implementation had 7.0F/11.0F single target
// impacts, 10 tick windup, 8 tick bounce and no blast radii at all).
const root = path.resolve(__dirname, "..");
const failures = [];

function read(relativePath) {
    const file = path.join(root, relativePath);
    if (!fs.existsSync(file)) {
        failures.push(`missing ${relativePath}`);
        return "";
    }
    return fs.readFileSync(file, "utf8");
}

function expect(content, pattern, description) {
    if (!pattern.test(content)) failures.push(description);
}

function refute(content, pattern, description) {
    if (pattern.test(content)) failures.push(description);
}

const events = read("src/main/java/alku/csrp/event/BookOfVengeanceEvents.java");
const item = read("src/main/java/alku/csrp/item/BookOfVengeanceItem.java");

// ItemBookOfVengeance constants.
expect(item, /private static final double RANGE = 32\.0D;/, "RANGE is no longer 32");
expect(item, /private static final String KB_NEXT_KEY = "srp_kb_next";/, "the srp_kb_next tag key changed");
expect(events, /GRAPPLE_COOLDOWN_TICKS = 60;/, "the grapple cooldown is no longer 60");
expect(events, /KNOCKBACK_COOLDOWN_TICKS = 80;/, "the knockback cooldown is no longer 80");
expect(events, /GRAPPLE_RANGE = 32\.0D;/, "GRAPPLE_RANGE is no longer 32");
expect(events, /KB_STRENGTH = 2\.2D;/, "KB_STRENGTH is no longer 2.2");
expect(events, /KB_UP = 0\.4D;/, "KB_UP is no longer 0.4");
expect(events, /KB_SELF_JUMP_Y = 0\.42D;/, "KB_SELF_JUMP_Y is no longer 0.42");
expect(events, /KB_RADIUS = 5\.0D;/, "the knockback radius is no longer 5");
expect(events, /KB_STRENGTH \* \(0\.45D \+ 0\.55D \* knockbackFalloff\(distance\)\)/,
        "the knockback falloff curve changed");
expect(events, /KB_UP \+ 0\.15D \* knockbackFalloff\(distance\)/, "the knockback lift curve changed");
expect(events, /1\.0D - Math\.min\(1\.0D, distance \/ KB_RADIUS\)/, "the falloff clamp changed");

// VengeanceGrappleHandler constants.
expect(events, /WINDUP_TICKS = 20;/, "the windup is no longer 20 ticks");
expect(events, /PULL_MAX_TICKS = 40;/, "the pull timeout is no longer 40 ticks");
expect(events, /PULL_SPEED = 1\.8D;/, "the pull speed is no longer 1.8");
expect(events, /CONTACT_DISTANCE = 2\.2D;/, "the contact distance is no longer 2.2");
expect(events, /IMPACT1_DAMAGE = 14\.0F;/, "the first impact damage is no longer 14.0F");
expect(events, /IMPACT1_RADIUS = 5\.0D;/, "the first impact radius is no longer 5");
expect(events, /IMPACT1_FORWARD = 1\.9D;/, "the first impact launch is no longer 1.9");
expect(events, /IMPACT1_UP = 0\.65D;/, "the first impact lift is no longer 0.65");
expect(events, /BOUNCE_TICKS = 14;/, "the bounce is no longer 14 ticks");
expect(events, /BOUNCE_PULL_SPEED = 2\.35D;/, "the bounce pull speed is no longer 2.35");
expect(events, /IMPACT2_TARGET_DAMAGE = 6\.0F;/, "the second impact target damage is no longer 6.0F");
expect(events, /IMPACT2_TARGET_UP = 0\.35D;/, "the second impact lift is no longer 0.35");
expect(events, /IMPACT2_RADIUS = 3\.5D;/, "the second impact radius is no longer 3.5");
expect(events, /IMPACT2_MAX_DAMAGE = 10\.0F;/, "the second impact blast damage is no longer 10.0F");
expect(events, /IMPACT2_KNOCKBACK = 0\.85D;/, "the second impact knockback is no longer 0.85");
expect(events, /EFFECT_DURATION_TICKS = 100;/, "the debuff duration is no longer 100 ticks");
expect(events, /VELOCITY_CLAMP_HORIZONTAL = 2\.8D;/, "the horizontal velocity clamp changed");
expect(events, /VELOCITY_CLAMP_VERTICAL = 2\.2D;/, "the vertical velocity clamp changed");
refute(events, /7\.0F|11\.0F/, "the old 7.0F / 11.0F impact damage values are back");

// Structure: three stages, both blasts, the launch, the scaled second blast and the pull semantics.
expect(events, /enum Phase \{\s*WINDUP,\s*PULL,\s*BOUNCE\s*\}/, "the phase set is no longer WINDUP/PULL/BOUNCE");
expect(events, /entity\.distanceTo\(target\) > IMPACT1_RADIUS/, "the first impact lost its radius check");
expect(events, /> IMPACT2_RADIUS/, "the second impact lost its radius check");
expect(events, /IMPACT2_MAX_DAMAGE \* scale/, "the second impact blast no longer scales with distance");
expect(events, /direction\.x \* IMPACT1_FORWARD/, "the first impact no longer launches along the look vector");
expect(events, /playerVelocity\.x \* 0\.15D/, "the first impact no longer damps the caster's velocity");
expect(events, /player\.setDeltaMovement\(vx, vy, vz\)/, "pullPlayerToward no longer sets the velocity directly");
expect(events, /timer >= 12/, "the bounce curve lost its first segment");
expect(events, /timer >= 6/, "the bounce curve lost its second segment");
expect(events, /pullPlayerToward\(player, target, BOUNCE_PULL_SPEED\)/, "the bounce tail no longer pulls again");
expect(events, /Math\.max\(velocity\.y, 0\.25D \+ 0\.2D \* scale\)/, "the second impact lift curve changed");
expect(events, /SoundEvents\.FIRE_EXTINGUISH/, "the lost-line-of-sight chain break no longer hisses");
expect(events, /applyVengeanceDebuffs/, "the first impact no longer debuffs what it hits");

// The five VENGEANCE_* sound events the original played; the ogg files and sounds.json entries already
// existed, but no sound event did.
const sounds = read("src/main/java/alku/csrp/registry/ModSounds.java");
for (const [field, id] of [["VENGEANCE_IMPACT", "vengeance.attack.impact"],
    ["VENGEANCE_PAPER", "vengeance.attack.paper"],
    ["VENGEANCE_CHAIN_IMPACT", "vengeance.attack.chain"],
    ["VENGEANCE_WHOOSH", "vengeance.attack.whoosh"],
    ["VENGEANCE_ROCK", "vengeance.attack.rock"]]) {
    expect(sounds, new RegExp(`${field} = register\\("${id.replace(/\./g, "\\.")}"\\)`),
            `ModSounds.${field} (${id}) is missing`);
}
const soundsJson = JSON.parse(read("src/main/resources/assets/csrp/sounds.json"));
for (const id of ["vengeance.attack.impact", "vengeance.attack.paper", "vengeance.attack.chain",
    "vengeance.attack.whoosh", "vengeance.attack.rock"]) {
    if (soundsJson[id] === undefined) failures.push(`sounds.json lost ${id}`);
}
// The resolved 1.12.2 sound fields the original also used.
expect(events, /SoundEvents\.ENCHANTMENT_TABLE_USE/, "the windup tick sound (BLOCK_ENCHANTMENT_TABLE_USE) is gone");
expect(events, /SoundEvents\.PLAYER_ATTACK_SWEEP/, "the knockback burst sound (ENTITY_PLAYER_ATTACK_SWEEP) is gone");
expect(events, /SoundEvents\.LIGHTNING_BOLT_THUNDER/, "the lightning sound (ENTITY_LIGHTNING_THUNDER) is gone");
expect(events, /SoundEvents\.GENERIC_EXPLODE/, "the second impact sound (ENTITY_GENERIC_EXPLODE) is gone");

// Triggers: plain use grapples the look target, sneak use bursts; both entity paths lead to the same pair.
expect(item, /public InteractionResult use\(Level level, Player player, InteractionHand hand\)/,
        "the item no longer handles plain use");
expect(item, /LivingEntity target = findLookTarget\(player, RANGE\)/, "plain use no longer picks the look target");
expect(item, /private static LivingEntity findLookTarget\(Player player, double range\)/,
        "findLookTarget is missing");
expect(item, /ClipContext\.Block\.COLLIDER/, "the look ray no longer stops at blocks");
expect(item, /inflate\(0\.3D\)\.clip\(eye, end\)/, "the 0.3 hitbox growth for the look ray changed");
expect(item, /player\.hasLineOfSight\(best\)/, "the look target is no longer line-of-sight checked");
expect(item, /addCooldown\(stack, BookOfVengeanceEvents\.GRAPPLE_COOLDOWN_TICKS\)/,
        "the grapple no longer uses the vanilla cooldown");
expect(item, /tag\.getLongOr\(KB_NEXT_KEY, 0L\) > gameTime/, "the knockback cooldown is no longer read from the tag");
expect(item, /now \+ BookOfVengeanceEvents\.KNOCKBACK_COOLDOWN_TICKS/,
        "the knockback cooldown is no longer written to the tag");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("vengeance grapple contract ok");
