// =====================================================================
//  zeigmal — the card furniture
//  A box the cards live in, sorted into four compartments, and a shelf
//  that puts a theme's cards up where a child can see and take them.
//  For the same printer and the same card as the holder next door.
//
//  Numbers carry the same tags as case/zeigmal-case.scad:
//
//    [M]  measured          [R]  researched
//    [A]  ASSUMPTION        [K]  design decision
//    [G]  computed — do NOT edit by hand
//
//  Coordinates: x along the length of a thing, y away from the child, z up.
//  Both parts stand on the table at z = 0 and print in that orientation.
//
//  The one thing to know before changing anything: a card is 80 mm wide,
//  and the children wanted them side by side rather than overlapping, so a
//  shelf costs 88 mm per card and two is all a 220 mm bed will take. That is
//  why there are several small shelves instead of one long one, and why
//  they clip together (ADR 0010).
// =====================================================================


/* ---------- 0.  What to render ---------- */

// "box" | "shelf" | "shelves" | "row" | "all"
part = "all";
show_cards = true;

$fa = 3;
$fs = 0.4;


/* =====================================================================
   1.  THE CARD — the same one the holder is built for
   ===================================================================== */

card_w = 80.0;   // [M] Stefanie, 2026-09-14
card_h = 120.0;  // [A] from the box the set came in
card_t =  0.7;   // [A] 300 g paper in 2 x 125 µm laminate
pitch  =  1.0;   // [A] per card in a stack: the laminate bows a little


/* =====================================================================
   2.  WALLS AND AIR  [K] — the station's numbers, so they look alike
   ===================================================================== */

wall     = 2.4;   // every printed wall — 6 x 0.4, 3 perimeters
floor_t  = 3.0;   // the floor of the box and the foot of a shelf
clr      = 1.5;   // air beside a card in a compartment
corner_r = 5.0;   // the box's upright corners, as on the station's top
bed      = [220, 220, 250];   // [R] Ender 3 V2


/* =====================================================================
   3.  THE BOX  [K]
   ===================================================================== */

box_comps = 4;     // compartments, one theme each
box_comp  = 48;    // along the box: 48 cards per compartment
box_h     = 70;    // the cards stand 50 mm proud of this

// No scoops. The first drawing had a thumb scoop in each compartment's
// long walls; with 50 mm of card standing proud there is nothing for them
// to do, and they only weaken the walls.


/* =====================================================================
   4.  THE SHELF  [K]
   ===================================================================== */

sh_n      = 2;     // cards side by side — two is what the bed allows
sh_pitch  = 88;    // card to card, so no card hides any part of another
sh_lean   = 12;    // degrees back from vertical
sh_slot   = 8.0;   // slot width: the one on show plus its spares behind
sh_depth  = 13;    // how deep the slot holds a card
sh_bar_d  = 18;    // the slotted bar, front to back
sh_bar_h  = 18;    // and tall
sh_slot_y = 7.0;   // where the slot sits in the bar, from its front
sh_front  = 25;    // how far the foot reaches IN FRONT of the bar. Without
                   // it a pull at the top of a card tips the shelf at 0.3 N,
                   // which is the weight of a sheet of paper
sh_base   = 69;    // the foot, front edge to back edge
plate_deg = 12;    // the bar's front leans back this much: the name plate
window_rim = 5.0;  // the rim left round the window that lightens the foot

// The tab that joins one shelf to the next, so a row of them stays a row
// when a child pulls a card out of the middle.
tab_w   = 12.0;   // along x, how far it sticks out
tab_d   = 10.0;   // across
tab_clr =  0.3;   // per side in the socket


/* =====================================================================
   5.  DERIVED  [G] — do not edit
   ===================================================================== */

box_inner_w = card_w + 2 * clr;                                   // 83
box_inner_l = box_comps * box_comp + (box_comps - 1) * wall;      // 199.2
box_l = box_inner_l + 2 * wall;                                   // 204
box_w = box_inner_w + 2 * wall;                                   // 87.8
box_cards = box_comps * floor(box_comp / pitch);                  // 192

sh_l = card_w + (sh_n - 1) * sh_pitch + 2 * wall;                 // 172.8
sh_cards_per_slot = floor(sh_slot / pitch);                       // 8
sh_bar_y      = sh_front;                             // the bar's front face
sh_plate_back = (sh_bar_h - floor_t) * tan(plate_deg);  // how far it leans
sh_front_wall = sh_slot_y - sh_plate_back;   // what is left of it at the top
sh_plate_h    = (sh_bar_h - floor_t) / cos(plate_deg);  // the name plate itself

echo(str("box       ", box_l, " x ", box_w, " x ", box_h + floor_t,
         " mm, ", box_comps, " x ", floor(box_comp / pitch), " = ", box_cards, " cards"));
echo(str("shelf     ", sh_l, " x ", sh_base, " x ", sh_bar_h,
         " mm, ", sh_n, " on show, ", sh_n * sh_cards_per_slot, " in all"));
echo(str("shelf     ", sh_bar_h + card_h * cos(sh_lean), " mm tall with cards; ",
         "name plate ", sh_plate_h, " mm, front wall ",
         sh_front_wall, " mm at its thinnest"));
echo(str("row       ", sh_n * 3, " cards on three shelves = ", 3 * sh_l + 2 * tab_w, " mm of table"));


/* =====================================================================
   6.  HELPERS
   ===================================================================== */

module rounded_rect(w, h, r) {
    hull() for (x = [r, w - r], y = [r, h - r]) translate([x, y]) circle(r);
}

module card(a = 1.0) {               // width along x, thickness along y
    color("Goldenrod", a) cube([card_w, card_t, card_h]);
}

module card_filed(a = 1.0) {         // thickness along x, width along y
    color("Goldenrod", a) cube([card_t, card_w, card_h]);
}


/* =====================================================================
   7.  THE BOX
   ===================================================================== */

module box() {
    difference() {
        linear_extrude(box_h + floor_t) rounded_rect(box_l, box_w, corner_r);
        translate([wall, wall, floor_t]) linear_extrude(box_h + 1)
            rounded_rect(box_inner_l, box_inner_w, max(corner_r - wall, 1.2));
    }
    // the dividers, full height so a theme stays a theme
    if (box_comps > 1)
        for (i = [1 : box_comps - 1])
            translate([wall + i * box_comp + (i - 1) * wall, wall, floor_t])
                cube([wall, box_inner_w, box_h]);

    if (show_cards)
        for (i = [0 : box_comps - 1], k = [0 : 11])
            translate([wall + i * (box_comp + wall) + 2 + k * 3.6, wall + clr, floor_t])
                card_filed();
}


/* =====================================================================
   8.  THE SHELF
   ===================================================================== */

// The bar's cross section, seen from the end: the foot runs back, the bar
// stands at the front, and the bar's face leans back as it rises — that
// face is the name plate, and leaning it back is also what keeps it from
// being an overhang.
function bar_profile() = [
    [0, 0],
    [sh_base, 0],
    [sh_base, floor_t],
    [sh_bar_y + sh_bar_d, floor_t],
    [sh_bar_y + sh_bar_d, sh_bar_h],
    [sh_bar_y + sh_plate_back, sh_bar_h],
    [sh_bar_y, floor_t],
    [0, floor_t]
];

module shelf_body() {
    translate([0, 0, 0]) rotate([90, 0, 90]) linear_extrude(sh_l) polygon(bar_profile());
}

// A tab on the right end, a socket on the left, so shelves make a row.
module shelf_tab() {
    translate([sh_l, (sh_base - tab_d) / 2, 0]) cube([tab_w, tab_d, floor_t]);
}

module shelf_socket() {
    translate([-1, (sh_base - tab_d) / 2 - tab_clr, -1])
        cube([tab_w + 1 + tab_clr, tab_d + 2 * tab_clr, floor_t + 2]);
}

// The foot behind the bar carries nothing but its own weight; a window in
// it takes a sixth off the print and leaves a rim all round. It clears the
// socket at the left end and the tab's root at the right.
module shelf_window() {
    translate([tab_w + window_rim, sh_bar_y + sh_bar_d + window_rim, -1])
        cube([sh_l - tab_w - 2 * window_rim,
              sh_base - sh_bar_y - sh_bar_d - 2 * window_rim, floor_t + 2]);
}

module shelf() {
    difference() {
        union() {
            shelf_body();
            shelf_tab();
        }
        shelf_socket();
        shelf_window();
        for (i = [0 : sh_n - 1])
            translate([wall + i * sh_pitch - clr, sh_bar_y + sh_slot_y, sh_bar_h])
                rotate([-sh_lean, 0, 0]) translate([0, 0, -sh_depth])
                    cube([card_w + 2 * clr, sh_slot, sh_depth + 20]);
    }
    if (show_cards)
        for (i = [0 : sh_n - 1])
            translate([wall + i * sh_pitch, sh_bar_y + sh_slot_y + 0.6, sh_bar_h])
                rotate([-sh_lean, 0, 0]) translate([0, 0, -sh_depth + 2]) card();
}


/* =====================================================================
   9.  RENDER
   ===================================================================== */

if (part == "box") box();
else if (part == "shelf") shelf();
else if (part == "shelves") for (k = [0 : 2]) translate([0, k * (sh_base + 10), 0]) shelf();
else if (part == "row") for (k = [0 : 2]) translate([k * (sh_l + tab_w), 0, 0]) shelf();
else if (part == "all") {
    box();
    translate([0, box_w + 30, 0]) for (k = [0 : 2]) translate([k * (sh_l + tab_w), 0, 0]) shelf();
}
else echo(str("unknown part \"", part, "\""));
