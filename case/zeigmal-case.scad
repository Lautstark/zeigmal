// =====================================================================
//  zeigmal — the holder
//  A Galaxy A51 lying landscape on a leaning block, a card slot cut into
//  the block's top edge right behind the phone, and a frame screwed on
//  from underneath so that a child cannot take the phone out.
//  Two printed parts, two M2 screws. PLA, 0.4 mm nozzle, no supports.
//
//  This is the second holder. The first (git tag holder-v1, printed on
//  2026-09-15) had a plate 20 mm wider than the phone, a floor apron behind,
//  a block for the funnel bolted onto its back and a frame that flexed. What
//  it got right — the funnel a card slides down into — is kept unchanged.
//  Every number carries a tag saying where it comes from:
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
coil_from_top = 12.5;  // [M] Stefanie, 2026-09-18: the printed slot took the
                       // card 2.5 mm deeper than it needed to, so the coil
                       // sits that much higher than the first guess
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
// Sticker centre from the card's LEFT edge, seen from the front. The body
// printed on 2026-09-15 was designed with the sticker on the centre line,
// card_w / 2, which is why its slot is centred 20 mm from the phone's end
// and the plate is 20 mm wider than the phone. The stickers actually go in
// the lower-left corner, 12.5, and read there. Keep card_w / 2 while the
// printed body is the body; set 12.5 before a new body is printed and the
// slot moves inside the phone's length.
sticker_x = 12.5;   // [K] the lower-left corner; holder-v1 used card_w / 2

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
// How far the plate stands above the frame's top rail is exactly this number,
// and that is the whole reason the block is taller than the phone. 15 was
// generous; the floor is mouth_ramp_h, because the front ramp has to fit
// above the frame's rail and not cut into it.
mouth_h       = 8.0;
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

// A wedge: the plate leans `tilt` back, the top is a ridge as thick as the
// funnel needs, and the back slopes down to the table at `slope_deg`. Hollow
// underneath, two ribs. Nothing sticks out of it.
tilt       = 20.0;  // degrees back from vertical
slope_deg  = 60.0;  // the back, from the horizontal; its underside is then a
                    // 30 degree overhang when printed base down, 45 is the limit
foot_h     =  8.0;  // the phone's bottom edge this far above the floor slab —
                    // this is the frame's foot, and the screws bite into it
corner_r   =  5.0;  // the body's and the frame's top corners, seen from the
                    // front. 8.0 left the top-left screw's counterbore only a
                    // quarter millimetre inside the arc, and it broke out
rib_x      = [52.0, 108.0];  // two ribs under the plate, x from the phone's left end
rib_t      = 2.4;


/* =====================================================================
   5.  THE FRAME  [K]
   ===================================================================== */

frame_face  = 2.4;   // the face over the screen's border. 1.6 was flimsy
frame_r     = 3.0;   // the frame's outer corners
frame_wall  = 2.4;   // the rails round the phone's edges
top_wall    = 6.4;   // the top rail is wider: two screws go through it into the plate
frame_over  = 3.0;   // how far the lip reaches over the screen. 2 covered the
                     // black border only; 3 holds better and is Stefanie's call,
                     // 2026-09-16, even where it takes half a millimetre of picture
lip_r       = phone_corner_r - frame_over;  // [G] the lip's inner corners follow the phone's
// One window over all three keys. The volume end was fine at 3.0; it was the
// power key, at the right end, that the rail overlapped by about 1 mm.
key_win_left   = 3.0;   // [K] left of the first volume key
key_win_right  = 6.0;   // [K] right of the power key, 3 mm more than the rest
// The openings in the right rail, [u0, u1, margin, wrap]: the feature's own
// extent up the plane, the air around it, and how far the opening reaches
// from the phone's right edge back over the face — a plug needs room, a
// speaker wants to fire forward and not sideways past the child.
// The microphone at mic_u stays covered: the station never records.
ports = [[jack_u[0],    jack_u[1],    3.0, 6.0],     // a 3.5 mm plug and its barrel
         [usb_u[0],     usb_u[1],     4.0, 6.0],     // a USB-C plug, angled or straight
         [speaker_u[0], speaker_u[1], 1.5, 9.0]];    // tight, and well round the corner

// The top rail is screwed to the plate from the front where the plate is
// solid behind it: one at the far left, beside the slot, two on the right
// past the funnel. The body has the tap holes. [x, u] from the phone's
// left end and bottom edge.
top_screws    = [[2.0, 76.5], [100.0, 77.1], [150.0, 77.1]];   // [K]
top_cb_d      = 4.2;             // counterbore for the head, in the rail
top_cb_depth  = 6.5;             // deep enough for M2 x 10 to bite the plate

// M2 screws — the ones on the shelf. From underneath, through the floor
// slab, into the frame's foot.
screw_d       = 2.0;   // [R] M2
screw_tap_d   = 1.8;   // [K] hole the screw cuts its own thread into, PLA;
                       //     1.6 came out of the printer too small to find
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
slot_x0  = coil_x - sticker_x - clr;          // the slot's left wall, inside: the sticker lands on the coil
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
u_top    = phone_h + play + top_wall + mouth_h;   // the funnel starts at the frame's top edge
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

// The frame in depth: the face sits in front of the phone, the foot under it.
face_n0  = phone_t + play;
face_n1  = phone_t + play + frame_face;
foot_n   = face_n1;

// The floor slab's front edge sits behind the frame's face, with play: the
// face reaches down to the table in front of it, so from the front nothing
// of the slab shows. u_at(z, n): where the plane frame's u is at world z.
function u_at(z, n) = (z - z0 - n * s) / c;
y_front  = py(u_at(floor_t, face_n0), face_n0) + play;

// The stop must be inside the plate (above the floor), the slot must fit
// between the plate's ends.
plate_u_at_floor = (floor_t - z0) / c;

// The frame's edges up the plane.
u_bot  = -play;                  // the phone's bottom edge, with play
u_topr = phone_h + play;         // the phone's top edge, with play
u_hi   = u_topr + top_wall;      // the frame's top edge

// The ridge: at the top the wedge is as thick as the plate plus the funnel's
// rear flare plus a wall. The back slopes from the ridge's back edge down to
// the table; where it lands is the base's back edge.
ridge_t  = plate_t + mouth_flare_n + wall;
ridge_y  = py(u_top, -ridge_t);
ridge_z  = pz(u_top, -ridge_t);
slope_y  = ridge_y + ridge_z / tan(slope_deg);
y_back   = slope_y;
base_depth = y_back - y_front;                // [G]

// The cavity underneath stops one wall below where the funnel starts, so the
// ridge with the funnel in it is solid (infill, in practice).
cavity_u_top = mouth_u0 - wall;

echo(str("channel   ", channel, " mm front to back, card ", card_t, " + sticker ", sticker_t, " + 2 x ", clr));
echo(str("plate     ", plate_t, " mm thick = window ", win_t, " + channel + rear wall ", back_t));
echo(str("insert    ", insert, " mm of card in the slot, ", card_proud, " mm standing proud"));
echo(str("slot      x ", slot_x0 - wall, " .. ", slot_x0 + slot_w + wall, " of plate ", plate_x_left, " .. ", plate_x_right));
echo(str("card      stands ", max(0, -(slot_x0 + clr)), " mm past the phone's left end"));
echo(str("read path ", win_t + clr + sticker_t, " mm from back glass to sticker face"));
echo(str("body      ", plate_x_right - plate_x_left, " x ", base_depth, " x ", pz(u_top, -plate_t) , " mm (l x d x h)"));
echo(str("base      ", base_depth, " deep: slab front at y ", y_front, ", back edge at ", y_back, ", ridge ", ridge_t, " thick"));
echo(str("with card ", pz(slot_u0 + card_h, -win_t - channel), " mm high"));
echo(str("frame     ", x_right - x_left, " x ", u_top - mouth_h + play + top_wall, " x ", face_n1, " mm"));
top_bite = screw_l - (face_n1 - top_cb_depth);   // [G] thread in the plate
echo(str("top screw M2 x ", screw_l, " bites ", top_bite, " mm of the ", plate_t, " mm plate"));
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

// The wedge's side profile in world (y, z): floor slab, up the plate's
// back, along the ridge, down the slope. The plate itself is a separate
// solid in the plane frame and overlaps this.
function base_profile() = [
    [y_front, 0],
    [y_front, floor_t],
    [py(plate_u_at_floor, -plate_t), floor_t],
    [py(u_top, -plate_t), pz(u_top, -plate_t)],
    [ridge_y, ridge_z],
    [y_back, 0]
];

// The cavity under the slope, before the walls are taken off it: up the
// plate's back to cavity_u_top, across to the slope, down the slope, and
// below the table so that shrinking it by one wall leaves the bottom open.
// The plate is a 6.3 mm wall already, so the plate-side edge starts one
// wall INSIDE the plate and the shrink brings it back to the plate's back.
function slope_y_at(z) = ridge_y + (ridge_z - z) / tan(slope_deg);
function cavity_profile() = [
    [py(u_at(-5, -plate_t + wall), -plate_t + wall), -5],
    [py(cavity_u_top, -plate_t + wall), pz(cavity_u_top, -plate_t + wall)],
    [slope_y_at(pz(cavity_u_top, -plate_t + wall)), pz(cavity_u_top, -plate_t + wall)],
    [slope_y_at(-5), -5]
];

// The ridge is hollow too, between the plate's back and the back wall,
// except where the funnel's rear wall needs the material: the funnel plus
// one wall around it is kept.
module ridge_hollow() {
    plane() difference() {
        pbox(x_left + wall, x_right - wall, cavity_u_top - 1, u_top - wall, -ridge_t + wall, -plate_t);
        hull() {
            pbox(slot_x0 - wall, slot_x0 + slot_w + wall, mouth_u0 - wall, mouth_u0 + 0.01,
                 -win_t - channel - wall, -win_t);
            pbox(slot_x0 - mouth_flare_x - wall, slot_x0 + slot_w + mouth_flare_x + wall, u_top, u_top + 1,
                 -win_t - channel - mouth_flare_n - wall, -win_t);
        }
    }
}

module base_solid() {
    translate([x_left, 0, 0]) rotate([90, 0, 90]) linear_extrude(x_right - x_left)
        polygon(base_profile());
}

module base_hollow_2d() {
    offset(delta = -wall) polygon(cavity_profile());
}

// Subtracted bay by bay between the ribs, so the ribs and the two ends stay.
module base_hollow() {
    bays = concat([x_left + wall], [for (r = rib_x) each [r - rib_t / 2, r + rib_t / 2]], [x_right - wall]);
    for (i = [0 : 2 : len(bays) - 2])
        translate([bays[i], 0, 0]) rotate([90, 0, 90]) linear_extrude(bays[i + 1] - bays[i])
            base_hollow_2d();
}

module plate() {
    plane() pbox(x_left, x_right, plate_u_at_floor - 5, u_top, -plate_t, 0);
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

module camera_relief() {
    plane() pbox(cam_x0 - cam_play, cam_x1 + cam_play, cam_u0 - cam_play, cam_u1 + cam_play,
                 -cam_h - cam_play, 0.01);
}

// The screw from below stands in the middle of where the foot meets the slab.
function screw_y() = (y_front + py(plate_u_at_floor, 0)) / 2;

// Screws from below: head in a counterbore under the slab, clearance through
// it; the tap hole is in the frame's foot. Screws from the front: tap holes
// in the plate where the top rail's screws land.
module screw_holes_body() {
    for (x = screw_x) translate([x, screw_y(), 0]) {
        translate([0, 0, -1]) cylinder(d = screw_clear_d, h = floor_t + 2);
        translate([0, 0, -1]) cylinder(d = screw_head_d + 0.4, h = 1 + cb_depth);
    }
    for (sc = top_screws)
        plane() translate([sc[0], sc[1], -top_bite - 1])
            cylinder(d = screw_tap_d, h = top_bite + 2);
}

// Seen from the front, the top corners are rounded; the bottom stands on
// the table and stays square. In the plane frame, extruded through the
// whole depth.
module top_rounded(w, h, r) {
    hull() {
        translate([0, 0]) square([w, h - r]);
        for (x = [r, w - r]) translate([x, h - r]) circle(r);
    }
}

module body() {
    difference() {
        intersection() {
            union() {
                base_solid();
                intersection() {
                    plate();
                    translate([x_left, y_front, 0]) cube([x_right - x_left, base_depth, 200]);
                }
            }
            plane() translate([x_left, -80, -80]) linear_extrude(100)
                top_rounded(x_right - x_left, u_top + 80, corner_r);
        }
        base_hollow();
        ridge_hollow();
        slot_cut();
        camera_relief();
        screw_holes_body();
    }
}


/* =====================================================================
   9.  THE FRAME
   ===================================================================== */

// Built in the plane frame. The face reaches down to the table in front of
// the floor slab; the foot sits on the slab. Two cuts by world planes do
// that, so the solids and the cuts are separate modules.
module frame_face_solids() {
    // the face: over the whole front, down past the table, the window is cut later
    pbox(x_left, x_right, u_bot - 40, u_hi, face_n0, face_n1);
    // left and right rails
    pbox(x_left, -play, u_bot - frame_wall, u_hi, 0, face_n1);
    pbox(phone_l + play, x_right, u_bot - frame_wall, u_hi, 0, face_n1);
    // top rail, wide enough for the two screw seats
    pbox(x_left, x_right, u_topr, u_hi, 0, face_n1);
    // the bottom rail, in front of the phone's bottom edge
    pbox(x_left, x_right, u_bot - frame_wall, u_bot, 0, face_n1);
}

module frame_foot_solid() {
    pbox(x_left, x_right, u_bot - 40, u_bot, 0, face_n0);
}

module frame_cuts() {
    // the window in the face: the screen minus the overlap, its corners
    // rounded like the phone's
    translate([frame_over, frame_over, face_n0 - 1]) linear_extrude(face_n1 - face_n0 + 2)
        rounded_rect(phone_l - 2 * frame_over, phone_h - 2 * frame_over, lip_r);
    // one window over the volume keys and the power key: through the top
    // rail and the face above the phone's edge, never into the lip
    pbox(key_vol_up[0] - key_win_left, key_power[1] + key_win_right,
         u_topr - 0.01, u_hi + 1, -1, face_n1 + 1);
    // the right end: jack, USB-C plug and speaker, through rail and face
    for (pt = ports)
        pbox(phone_l - pt[3], x_right + 1, pt[0] - pt[2], pt[1] + pt[2], -1, face_n1 + 1);
    // two screws from the front through the top rail into the plate:
    // clearance hole, and a counterbore so M2 x 10 reaches the plate
    for (sc = top_screws) translate([sc[0], sc[1], 0]) {
        translate([0, 0, -1]) cylinder(d = screw_clear_d, h = face_n1 + 2);
        translate([0, 0, face_n1 - top_cb_depth]) cylinder(d = top_cb_d, h = top_cb_depth + 1);
    }
    // world-vertical holes for the two screws from below: the inverse of
    // plane(), applied to a cylinder standing at the screw's world position
    for (x = screw_x)
        rotate([-(90 - tilt), 0, 0]) translate([x, screw_y() - y0, -1 - z0])
            cylinder(d = screw_tap_d, h = screw_l + 1);
}

module above(z) {
    translate([x_left - 1, -100, z]) cube([x_right - x_left + 2, 300, 300]);
}

module frame() {
    difference() {
        intersection() {
            union() {
                intersection() { plane() frame_face_solids(); above(0); }
                intersection() { plane() frame_foot_solid(); above(floor_t); }
            }
            // rounded top corners, the same radius as the body's
            plane() translate([x_left, -80, -1]) linear_extrude(face_n1 + 2)
                top_rounded(x_right - x_left, u_hi + 80, corner_r);
        }
        plane() frame_cuts();
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
            cube([slot_w + 2 * wall + 6, ridge_y - y_front + 12, 300]);
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
    translate([0, 0, face_n1]) rotate([180, 0, 0])
        rotate([-(90 - tilt), 0, 0]) translate([0, -y0, -z0]) frame();
}
