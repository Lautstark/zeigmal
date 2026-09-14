// =====================================================================
//  zeigmal — the holder
//  A Galaxy A51 lying landscape on a leaning block, a card slot cut into
//  the block's top edge right behind the phone, and a frame screwed on
//  from underneath so that a child cannot take the phone out.
//  Two printed parts, two M2 screws. PLA, 0.4 mm nozzle, no supports.
//
//  IMPORTANT: the phone has not been measured yet and the coil has not been
//  found. Every number carries a tag saying where it comes from:
//
//    [M]  measured (nobody, yet — see docs/hardware.md)
//    [R]  researched (manufacturer figure)
//    [A]  ASSUMPTION — unverified. Measure on the real part!
//    [K]  design decision — freely choosable
//    [G]  computed — do NOT edit by hand, follows from other values
//
//  The two numbers most likely wrong once the phone is on the desk are
//  `coil_x` and `coil_from_top` (section 1): where the NFC coil sits behind
//  the back glass. E4 in docs/experiments.md measures them. The slot's
//  position and the insertion depth follow from them and nothing else
//  has to change.
//
//  Coordinates. The world frame: x runs along the phone's length and +x is
//  the CHILD'S RIGHT; +y runs AWAY from the child; z is up and z = 0 is the
//  table. The phone lies on a plane leaning `tilt` degrees back. Most of the
//  geometry is built in the PLANE frame and then tilted by `plane()`:
//
//    x  along the phone, 0 at the phone's left end (the camera end)
//    u  up the plane, 0 at the phone's bottom edge
//    n  out of the plane, 0 at the phone's BACK face, +n toward the child
//
//  In the plane frame the plate lies at n <= 0, the phone at 0 <= n <=
//  phone_t, and the frame in front of that. The card slot is a pocket at
//  n < 0, i.e. inside the plate, behind the phone.
// =====================================================================


/* ---------- 0.  What to render ---------- */

// "body" | "frame" | "assembly" | "exploded" | "fit-test" | "frame-corner"
// | "coupons" | "printbed" | "frame-placed"
// "frame" comes out face down, ready to print; "frame-placed" is where it
// sits on the body.
part = "assembly";

// Draw the phone and a card in the assembly views
show_phone = true;
show_card  = true;

$fa = 3;
$fs = 0.4;


/* =====================================================================
   1.  THE PHONE — Samsung Galaxy A51, SM-A515F, bare, no case  [K]
   ===================================================================== */

phone_l = 158.9;   // [R] length, the long edge — verify with calipers
phone_h = 73.6;    // [R] width, the short edge — in landscape this is the height
phone_t =  7.9;    // [R] thickness at the flat part of the back
phone_corner_r = 8.0;  // [A] corner radius of the outline

// How wide the black border round the AMOLED is. The frame may cover this
// much of the front without hiding pixels.
screen_inset = 2.5;    // [A]

// The camera island stands proud of the back glass. In landscape with the
// port to the child's right it sits at the top-left, near the slot.
cam_x0 =  8.0;   // [A] from the phone's left end
cam_x1 = 30.0;   // [A]
cam_u0 = phone_h - 28.0;  // [A] from the phone's bottom edge
cam_u1 = phone_h -  6.0;  // [A]
cam_h  =  1.2;   // [A] how far it stands proud
cam_play = 0.3;  // [K] air around it in its relief pocket — what is left of
                 //     win_t under the pocket is the thinnest wall in the part

// Keys on the TOP long edge (the phone's portrait right edge). x from the
// camera end. The frame's top rail gets a window over each volume key and a
// shallow relief over the power key so that it is neither pressed nor
// reachable.
// The first frame corner's rail reached 2 mm over the volume key, so the
// window starts 2 mm earlier than the first guess (Stefanie, 2026-09-14).
// Both volume keys share one window; the exact positions are still [A].
key_vol_up = [28.0, 38.0];   // [A]
key_vol_dn = [40.0, 48.0];   // [A]
key_power  = [56.0, 66.0];   // [A]
key_proud  =  0.5;           // [A] how far a key stands out of the edge

// The RIGHT end (the phone's portrait bottom edge), u from the bottom.
// Everything here must stay open: the speaker fires out of it.
jack_u    = [ 8.0, 14.0];   // [A] 3.5 mm jack
mic_u     = [21.0, 23.0];   // [A]
usb_u     = [30.0, 40.0];   // [A] USB-C
speaker_u = [55.0, 68.0];   // [A]

// >>> THE two numbers E4 measures. <<<
// Where the NFC coil's centre sits behind the back glass, in landscape.
// Stefanie, 2026-09-13, by eye: in the corner by the camera. The centre of
// the reliable area is still E4's to find; these put the slot in that corner.
coil_x        = 20.0;  // [A] from the phone's left end
coil_from_top = 15.0;  // [A] below the phone's top edge
coil_r        = 15.0;  // [A] radius of the area that reads 5 of 5 (E4)


/* =====================================================================
   2.  THE CARD AND THE STICKER
   ===================================================================== */

card_w = 80.0;   // [A] the bought set is "8 x 12 cm" — measure
card_h = 120.0;  // [A]
card_t =  0.7;   // [A] 300 g paper in 2 x 125 µm laminate

sticker_d =  25.0;  // [K] NTAG213, 25 mm round (docs/nfc.md)
sticker_t =   0.2;  // [A] paper-face sticker

// Where the sticker goes on every card: on the width centre line so that a
// card pushed in back to front still reads, and this far above the bottom
// edge. The slot's stop puts it over the coil — that is what `insert` (G)
// below is for.
sticker_y = 30.0;   // [K] sticker centre above the card's bottom edge. 20 on
                    // the first fit test let the card lean a degree in the
                    // slot; 10 mm deeper is what Stefanie asked for

// A child must be able to take the card back without a fingernail.
card_proud_min = 45.0;  // [K] docs/enclosure.md


/* =====================================================================
   3.  CLEARANCES AND WALLS  [K]
   ===================================================================== */

clr      = 0.3;   // the card in its slot, per side           <-- the one to tune
play     = 0.3;   // the phone in its frame, per side
wall     = 2.4;   // every printed wall — 6 x 0.4, 3 perimeters
floor_t  = 3.0;   // the body's floor slab, the screws go through it; the
                  // head sinks into it from below, see cb_depth
chamfer  = 0.8;   // lead-in at the slot mouth
// The mouth: the plate rises `mouth_h` above the frame's top rail, and over
// that height the slot opens out like a funnel — `mouth_flare_x` wider on
// each side, `mouth_flare_n` deeper front and back. At the front the flare
// reaches the plate's face, so a child can put the card flat against the
// wall above the phone and slide it down; the funnel finds the slot.
mouth_h       = 15.0;
mouth_flare_x =  6.0;
mouth_flare_n =  5.0;
mouth_ramp_h  =  6.0;   // the front ramp only over the top part, so the
                        // plate's top edge stays thick and not a knife

// The wall between the phone's back and the card. E5 says whether 2.4 reads
// or whether this has to come down to 1.0. It is the whole slot's front
// wall, not a window in it: a window would be a thin patch inside a thick
// plate printed at 70 degrees, and that is the weakest place in any part.
win_t   = 2.4;   // [A] E5 decides
back_t  = 2.4;   // the slot's rear wall, the one the card leans on


/* =====================================================================
   4.  THE STAND  [K]
   ===================================================================== */

tilt       = 20.0;  // degrees back from vertical
base_depth = 110.0; // front edge to back edge, on the table. verify.py says
                    // what sideways push at the card's top tips the holder;
                    // 110 accepts a light one (ADR 0007) — the children push
                    // cards in, they do not shove them
rear_u     = 46.0;  // where the rear slope leaves the plate's back, up the plane
slope_deg  = 50.0;  // the rear slope from the horizontal. Printed base down
                    // its underside is a 40 degree overhang; 45 is the limit
foot_h     =  8.0;  // the phone's bottom edge this far above the floor slab —
                    // this is the frame's foot, and the screws bite into it
front_margin = 4.0;  // floor slab in front of the frame's foot
rib_x      = [52.0, 108.0];  // two ribs under the plate, x from the phone's left end
rib_t      = 2.4;


/* =====================================================================
   5.  THE FRAME  [K]
   ===================================================================== */

frame_face  = 1.6;   // the lip over the screen, 8 layers
frame_r     = 3.0;   // the frame's outer corners
lip_r       = phone_corner_r - 2.0;  // [G] the lip's inner corners follow the phone's
frame_wall  = 2.4;   // the rails round the phone's edges
frame_over  = 2.0;   // how far the lip reaches over the screen
return_w    = 5.0;   // the two corner returns at the open right end
key_win_margin = 2.0;   // the volume window is both keys plus this each side
power_margin   = 3.0;   // the power relief is the key plus this each side
power_relief   = 1.0;   // the rail is hollowed this deep over the power key

// M2 screws — the ones on the shelf. From underneath, through the floor
// slab, into the frame's foot.
screw_d       = 2.0;   // [R] M2
screw_tap_d   = 1.6;   // [K] hole the screw cuts its own thread into, PLA
screw_clear_d = 2.3;   // [K] through-hole in the floor slab
screw_head_d  = 3.8;   // [R] pan head — measure yours
screw_head_h  = 1.4;   // [R]
screw_l       = 10.0;  // [K] what is on the shelf: M2 x 10
screw_x       = [14.0, phone_l - 14.0];  // [K] from the phone's left end
cb_depth      = screw_head_h + 0.2;      // [G] counterbore under the floor


/* =====================================================================
   6.  DERIVED  [G] — do not edit
   ===================================================================== */

channel  = card_t + sticker_t + 2 * clr;      // 1.5  the slot, front to back
plate_t  = win_t + channel + back_t;          // 6.3  the plate's full thickness

// The stop puts the sticker over the coil. That is the only reason the
// card goes in as far as it does.
insert     = coil_from_top + sticker_y;       // 40   how far the card goes in
card_proud = card_h - insert;                 // 80   what stands above the phone

slot_w   = card_w + 2 * clr;                  // 80.6
slot_x0  = coil_x - slot_w / 2;               // the slot's left wall, inside
slot_u0  = phone_h - insert;                  // the stop, up the plane

// The frame's ends, round the phone with play.
x_left   = -play - frame_wall;
x_right  = phone_l + play + frame_wall;

// The plate: as wide as the frame, or wider where the slot and its funnel
// need it — a coil in the corner puts the card past the phone's end, and the
// plate follows the card, the frame does not. It rises `mouth_h` above the
// frame's top rail so the funnel has its height.
plate_x_left  = min(x_left, slot_x0 - mouth_flare_x - wall);
plate_x_right = max(x_right, slot_x0 + slot_w + mouth_flare_x + wall);
u_top    = phone_h + play + frame_wall + mouth_h;
mouth_u0 = u_top - mouth_h;                        // where the funnel starts
mouth_n_front = min(-win_t + mouth_flare_n, 0);    // the flare, clamped at the plate's face

// Where the plane frame sits in the world. The phone's bottom edge is
// foot_h above the floor slab; the plane's origin is that edge.
z0 = floor_t + foot_h;
y0 = 0;

s = sin(tilt);
c = cos(tilt);

// A point (u, n) in the plane frame, in world (y, z).
function py(u, n) = y0 + u * s - n * c;
function pz(u, n) = z0 + u * c + n * s;

// The frame's foot reaches this far forward; the floor slab starts a little
// in front of that.
foot_n   = phone_t + play + frame_face;
foot_u_at_floor = (floor_t - z0 - foot_n * s) / c;   // u where the foot's front meets the floor
y_front  = py(foot_u_at_floor, foot_n) - front_margin;
y_back   = y_front + base_depth;

// The stop must be inside the plate (above the floor), the slot must fit
// between the plate's ends.
plate_u_at_floor = (floor_t - z0) / c;

// The rear slope leaves the plate's back at rear_u and reaches the floor
// slab at slope_y; behind that the floor continues as the rear apron.
slope_y = py(rear_u, -plate_t) + (pz(rear_u, -plate_t) - floor_t) / tan(slope_deg);

echo(str("channel   ", channel, " mm front to back, card ", card_t, " + sticker ", sticker_t, " + 2 x ", clr));
echo(str("plate     ", plate_t, " mm thick = window ", win_t, " + channel + rear wall ", back_t));
echo(str("insert    ", insert, " mm of card in the slot, ", card_proud, " mm standing proud"));
echo(str("slot      x ", slot_x0 - wall, " .. ", slot_x0 + slot_w + wall, " of plate ", plate_x_left, " .. ", plate_x_right));
echo(str("card      stands ", max(0, -(slot_x0 + clr)), " mm past the phone's left end"));
echo(str("read path ", win_t + clr + sticker_t, " mm from back glass to sticker face"));
echo(str("body      ", plate_x_right - plate_x_left, " x ", base_depth, " x ", pz(u_top, -plate_t) , " mm (l x d x h)"));
echo(str("base      front apron ", py(plate_u_at_floor, 0) - y_front, ", slope foot at y ", slope_y, ", rear apron ", y_back - slope_y));
echo(str("with card ", pz(slot_u0 + card_h, -win_t - channel), " mm high"));
echo(str("frame     ", x_right - x_left, " x ", u_top - mouth_h + play + frame_wall, " x ", foot_n, " mm"));
echo(str("mouth     ", slot_w + 2 * mouth_flare_x, " wide, ", channel + mouth_flare_n + (mouth_n_front + win_t), " front to back at the top, ", mouth_h, " tall"));
floor_left   = floor_t - cb_depth;            // [G] floor under the screw head
screw_engage = screw_l - floor_left;          // [G] thread in the foot
echo(str("screw     M2 x ", screw_l, ": ", floor_left, " of floor under the head, ", screw_engage, " into the foot"));
echo(str("camera    ", win_t - cam_h - cam_play, " mm of wall between the camera pocket and the slot"));


/* =====================================================================
   7.  HELPERS
   ===================================================================== */

module plane() {
    translate([0, y0, z0]) rotate([90 - tilt, 0, 0]) children();
}

// A box in the plane frame: from (x0,u0,n0) to (x1,u1,n1)
module pbox(x0, x1, u0, u1, n0, n1) {
    translate([x0, u0, n0]) cube([x1 - x0, u1 - u0, n1 - n0]);
}

module rounded_rect(w, h, r) {
    hull() for (x = [r, w - r], y = [r, h - r]) translate([x, y]) circle(r);
}


/* =====================================================================
   8.  THE BODY
   ===================================================================== */

// The base's side profile in world (y, z): front apron, the plate's back,
// the rear slope, the rear apron. The plate itself is a separate solid in
// the plane frame.
function base_profile() = [
    [y_front, 0],
    [y_front, floor_t],
    [py(plate_u_at_floor, -plate_t), floor_t],
    [py(rear_u, -plate_t), pz(rear_u, -plate_t)],
    [slope_y, floor_t],
    [y_back, floor_t],
    [y_back, 0]
];

// The cavity under the slope, before the walls are taken off it: plate back,
// slope, and the floor — pushed below the table so that shrinking it by one
// wall leaves the bottom open.
function cavity_profile() = [
    [py((-5 - z0 + plate_t * s) / c, -plate_t), -5],
    [py(rear_u, -plate_t), pz(rear_u, -plate_t)],
    [slope_y, floor_t],
    [slope_y, -5]
];

// rotate([90, 0, 90]) turns the (y, z) profile so that it extrudes along
// +x; the extrusion starts at x = 0, hence the translate to the plate's end.
module base_solid() {
    translate([plate_x_left, 0, 0]) rotate([90, 0, 90]) linear_extrude(plate_x_right - plate_x_left)
        polygon(base_profile());
}

// Hollow underneath: the cavity shrunk by one wall, which leaves a wall
// behind the plate, the slope wall, a small solid foot where the slope
// meets the floor, and an open bottom. Subtracted bay by bay between the
// ribs, so the ribs and the two ends stay.
module base_hollow_2d() {
    offset(delta = -wall) polygon(cavity_profile());
}

module base_hollow() {
    bays = concat([plate_x_left + wall], [for (r = rib_x) each [r - rib_t / 2, r + rib_t / 2]], [plate_x_right - wall]);
    for (i = [0 : 2 : len(bays) - 2])
        translate([bays[i], 0, 0]) rotate([90, 0, 90]) linear_extrude(bays[i + 1] - bays[i])
            base_hollow_2d();
}

module plate() {
    plane() pbox(plate_x_left, plate_x_right, plate_u_at_floor - 5, u_top, -plate_t, 0);
}

module slot_cut() {
    plane() {
        // the channel
        pbox(slot_x0, slot_x0 + slot_w, slot_u0, mouth_u0 + 0.01, -win_t - channel, -win_t);
        // the funnel: from the channel's section at mouth_u0 to the flared
        // opening at the top, sides and back
        hull() {
            pbox(slot_x0, slot_x0 + slot_w, mouth_u0, mouth_u0 + 0.01, -win_t - channel, -win_t);
            pbox(slot_x0 - mouth_flare_x, slot_x0 + slot_w + mouth_flare_x, u_top, u_top + 1,
                 -win_t - channel - mouth_flare_n, -win_t + 0.01);
        }
        // the front ramp: over the top mouth_ramp_h the front wall falls
        // away to the plate's face, so a card slid down the face drops in
        hull() {
            pbox(slot_x0, slot_x0 + slot_w, u_top - mouth_ramp_h, u_top - mouth_ramp_h + 0.01,
                 -win_t - channel, -win_t);
            pbox(slot_x0 - mouth_flare_x, slot_x0 + slot_w + mouth_flare_x, u_top, u_top + 1,
                 -win_t - channel - mouth_flare_n, mouth_n_front + 0.01);
        }
    }
}

// The funnel's rear flare would run out of the plate's back; a block behind
// the plate at the mouth carries it, with a 45 degree underside so it
// prints without support.
module mouth_block() {
    plane() hull() {
        pbox(slot_x0 - mouth_flare_x - wall, slot_x0 + slot_w + mouth_flare_x + wall,
             mouth_u0, u_top, -plate_t - mouth_flare_n, -plate_t + 0.01);
        pbox(slot_x0 - mouth_flare_x - wall, slot_x0 + slot_w + mouth_flare_x + wall,
             mouth_u0 - mouth_flare_n, mouth_u0, -plate_t, -plate_t + 0.01);
    }
}

module camera_relief() {
    plane() pbox(cam_x0 - cam_play, cam_x1 + cam_play, cam_u0 - cam_play, cam_u1 + cam_play,
                 -cam_h - cam_play, 0.01);
}

// The screws come up through the floor slab. Head in a counterbore
// underneath, clearance through the slab; the tap hole is in the frame.
module screw_holes_body() {
    for (x = screw_x) translate([x, screw_y(), 0]) {
        translate([0, 0, -1]) cylinder(d = screw_clear_d, h = floor_t + 2);
        translate([0, 0, -1]) cylinder(d = screw_head_d + 0.4, h = 1 + cb_depth);
    }
}

// The screw stands in the middle of the foot, where the foot meets the floor.
function screw_y() = py(foot_u_at_floor, foot_n / 2) + 0.4;

module body() {
    difference() {
        union() {
            base_solid();
            mouth_block();
            intersection() {
                plate();
                // nothing below the table, nothing behind the base
                translate([plate_x_left, y_front, 0]) cube([plate_x_right - plate_x_left, base_depth, 200]);
            }
        }
        base_hollow();
        slot_cut();
        camera_relief();
        screw_holes_body();
    }
}


/* =====================================================================
   9.  THE FRAME
   ===================================================================== */

// Built in the plane frame; the foot is then cut flat by the floor slab.
module frame_raw() {
    face_n0 = phone_t + play;
    face_n1 = foot_n;
    u_bot = -play;                 // the phone's bottom edge, with play
    u_topr = phone_h + play;       // the phone's top edge, with play
    difference() {
        intersection() { union() {
            // the face: a plate over the whole front, the window is cut below
            pbox(x_left, x_right, u_bot - frame_wall, u_topr + frame_wall, face_n0, face_n1);
            // left rail
            pbox(x_left, -play, u_bot - frame_wall, u_topr + frame_wall, 0, face_n1);
            // top rail
            pbox(x_left, x_right, u_topr, u_topr + frame_wall, 0, face_n1);
            // the foot: bottom rail, reaching down to be cut by the floor
            pbox(x_left, x_right, u_bot - 40, u_bot, 0, face_n1);
            // corner returns at the open right end
            for (uu = [[u_bot - frame_wall, return_w], [phone_h - return_w, u_topr + frame_wall]])
                pbox(phone_l + play, x_right, uu[0], uu[1], 0, face_n1);
        }
        // ... all of it inside rounded outer corners
        translate([x_left, u_bot - frame_wall, -1]) linear_extrude(face_n1 + 2)
            rounded_rect(x_right - x_left, u_topr + frame_wall - (u_bot - frame_wall), frame_r);
        }
        // the window in the face: the screen minus the overlap, open to the
        // right, its corners rounded like the phone's
        translate([frame_over, frame_over, face_n0 - 1]) linear_extrude(face_n1 - face_n0 + 2)
            rounded_rect(x_right + 20 - frame_over, phone_h - 2 * frame_over, lip_r);
        // ... but the corner returns keep their lip
        // (they are inside the window cut above, so put them back below)
        // one window for both volume keys: through the top rail and the
        // face above the phone's edge, never into the lip over the screen
        pbox(key_vol_up[0] - key_win_margin, key_vol_dn[1] + key_win_margin,
             u_topr - 0.01, u_topr + frame_wall + 1, -1, face_n1 + 1);
        // power key: a relief in the rail's inner face, not a window
        pbox(key_power[0] - power_margin, key_power[1] + power_margin,
             u_topr - 0.01, u_topr + power_relief, -1, face_n0);
        // tap holes for the screws, up into the foot
        // world-vertical holes: the inverse of plane(), applied to a cylinder
        // standing at the screw's world position
        for (x = screw_x)
            rotate([-(90 - tilt), 0, 0]) translate([x, screw_y() - y0, -1 - z0])
                cylinder(d = screw_tap_d, h = screw_l + 1);
    }
    // corner returns' lips, put back after the window cut
    for (uu = [[u_bot - frame_wall, return_w], [phone_h - return_w, u_topr + frame_wall]])
        pbox(phone_l - frame_over, x_right, uu[0], uu[1], face_n0, face_n1);
}

module frame() {
    intersection() {
        plane() frame_raw();
        // cut the foot flat where it sits on the floor slab
        translate([x_left - 1, -100, floor_t]) cube([x_right - x_left + 2, 300, 300]);
    }
}


/* =====================================================================
   10.  DUMMIES, FOR THE ASSEMBLY VIEWS
   ===================================================================== */

module phone_dummy() {
    color("SlateGray") plane() translate([0, 0, 0]) linear_extrude(phone_t)
        rounded_rect(phone_l, phone_h, phone_corner_r);
    color("Black") plane() translate([screen_inset, screen_inset, phone_t]) linear_extrude(0.1)
        square([phone_l - 2 * screen_inset, phone_h - 2 * screen_inset]);
    color("DimGray") plane() pbox(cam_x0, cam_x1, cam_u0, cam_u1, -cam_h, 0);
}

module card_dummy() {
    color("Goldenrod") plane() pbox(slot_x0 + clr, slot_x0 + clr + card_w, slot_u0, slot_u0 + card_h,
                                    -win_t - channel + clr, -win_t - channel + clr + card_t);
    color("White") plane() translate([coil_x, slot_u0 + sticker_y, -win_t - channel + clr + card_t])
        cylinder(d = sticker_d, h = sticker_t);
}


/* =====================================================================
   11.  FIT TEST AND COUPONS
   ===================================================================== */

// The slot and the plate around it, standing at the real tilt: the slot
// clearance, the chamfer and the tilt in one print. It is the top of the
// body cut off level below the stop, so it stands on the cut and leans like
// the whole thing does; the frame corner covers the foot and the screw.
fit_cut_z = 40.0;   // [K] where the fit test is cut off, above the table
module fit_test() {
    translate([0, 0, -fit_cut_z]) intersection() {
        body();
        translate([slot_x0 - wall - 3, y_front - 1, fit_cut_z])
            cube([slot_w + 2 * wall + 6, py(rear_u, -plate_t) - y_front + 12, 300]);
    }
}

// The frame's bottom-left corner, 40 mm of it: the play round the phone and
// the lip over the screen, with the screw hole. Laid face down, as the whole
// frame prints.
module frame_corner() {
    intersection() {
        frame_flat();
        translate([x_left - 1, -100, -1]) cube([40, 200, 60]);
    }
}

// E5: three flat squares, 1 / 2 / 3 mm, for the read-through test.
module coupons() {
    for (i = [0 : 2]) translate([i * 46, 0, 0]) cube([40, 40, 1 + i]);
}


/* =====================================================================
   12.  RENDER
   ===================================================================== */

if (part == "body") body();
else if (part == "frame") frame_flat();          // face down, as it prints
else if (part == "frame-placed") frame();     // where it sits on the body
else if (part == "fit-test") fit_test();
else if (part == "frame-corner") frame_corner();
else if (part == "coupons") coupons();
else if (part == "printbed") {
    body();
    // the frame beside the body, face down on the bed
    translate([0, y_back + 20, 0]) frame_flat();
}
else if (part == "exploded") {
    body();
    translate([0, -30, 12]) frame();
    if (show_phone) translate([0, -12, 5]) phone_dummy();
    if (show_card) translate([0, 0, 40]) card_dummy();
}
else if (part == "assembly") {
    body();
    frame();
    if (show_phone) phone_dummy();
    if (show_card) card_dummy();
}
else if (part != "none") echo(str("unknown part \"", part, "\""));

// The frame as it lies on the bed: face down, foot pointing up. Undo the
// tilt, then TURN it over — a rotation, never mirror(): the first frame
// corner was printed as a reflection and only fitted the wrong end of the
// phone, foot up.
module frame_flat() {
    translate([0, 0, foot_n]) rotate([180, 0, 0])
        rotate([-(90 - tilt), 0, 0]) translate([0, -y0, -z0]) frame();
}
