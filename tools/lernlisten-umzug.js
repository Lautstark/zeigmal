// Moves zeigmal's two card sets in the app, SIGNbox 1 and Halloween, into
// SIGNdigital Lernlisten, once. Runs in a browser tab on sign-digital.de where
// you are logged in: open the console and paste this file. It uses that tab's
// login and sends nothing anywhere else.
//
// Before: make two empty Lernlisten on the site, named exactly
// "SIGNbox 1" and "Halloween" (Lernlisten → Neue Lernliste → Leer).
//
// First run as it is: DRY is true, so it only reads. It looks every slug up,
// finds the two lists, and prints what it would put into each and which
// slugs the site does not know. Read that. Then set DRY to false and paste it
// again: it fills each list with one PATCH, the way the site does when you
// add a sign. It only touches a list that is still empty.
//
// The dry run of 2026-11-08 found: Halloween 23 of 24 (verkleiden and Kostüm
// are one sign, so it is in the list once); SIGNbox 1 213 of 224, the ten
// slugs already known dead, and "schon", whose slug `schon` is the site's
// "schön" (both cards came out as one sign). Those eleven are added by hand on
// the site after the move.
//
// The words were taken from core/src/main/resources/{signbox1,halloween}.txt
// on 2026-11-08 with the app's own slug rule; those files go once the move is
// done, so this file is not kept in step with them.

const DRY = true;

const SETS = {
  "SIGNbox 1": [
    ["Abend(s)", "abend-s"],
    ["aber", "aber"],
    ["alle/s", "alle-s"],
    ["allein", "allein"],
    ["an/am", "an-am"],
    ["andere/s", "andere-s"],
    ["anrufen", "anrufen"],
    ["arbeiten", "arbeiten"],
    ["Arzt", "arzt"],
    ["auch", "auch"],
    ["auf", "auf"],
    ["aufräumen", "aufraumen"],
    ["aus", "aus"],
    ["bei", "bei"],
    ["bitte", "bitte"],
    ["böse", "bose"],
    ["bringen", "bringen"],
    ["Brot", "brot"],
    ["Bruder", "bruder"],
    ["Bus", "bus"],
    ["cool", "cool"],
    ["da", "da"],
    ["danke", "danke"],
    ["dann", "dann"],
    ["das", "das"],
    ["dein/e", "dein-e"],
    ["dem/den", "dem-den"],
    ["denken", "denken"],
    ["denn", "denn"],
    ["der", "der"],
    ["die", "die"],
    ["Dienstag", "dienstag"],
    ["doch", "doch"],
    ["Donnerstag", "donnerstag"],
    ["draußen", "draussen"],
    ["du/dir/dich", "du-dir-dich"],
    ["dumm", "dumm"],
    ["dürfen", "durfen"],
    ["Durst", "durst"],
    ["ein/eine", "ein-eine"],
    ["Entschuldigung", "entschuldigung"],
    ["er/ihm/ihn", "er-ihm-ihn"],
    ["es/ihm", "es-ihm"],
    ["essen", "essen"],
    ["fahren", "fahren"],
    ["falsch", "falsch"],
    ["fehlen", "fehlen"],
    ["fertig", "fertig"],
    ["finden", "finden"],
    ["fragen", "fragen"],
    ["Frau", "frau"],
    ["frech", "frech"],
    ["frei", "frei"],
    ["Freitag", "freitag"],
    ["Freund", "freund"],
    ["Frühling", "fruhling"],
    ["für", "fur"],
    ["Fußball", "fussball"],
    ["ganz", "ganz"],
    ["geben", "geben"],
    ["Geburtstag", "geburtstag"],
    ["gehen", "gehen"],
    ["gerade", "gerade"],
    ["gestern", "gestern"],
    ["gewesen/Partizip II", "gewesen"],
    ["gleich", "gleich"],
    ["glücklich", "glucklich"],
    ["groß", "gross"],
    ["gucken/sehen", "gucken-sehen-mg"],
    ["gut", "gut"],
    ["haben", "haben"],
    ["Hallo", "hallo"],
    ["heiß", "heiss"],
    ["Herbst", "herbst"],
    ["heute", "heute"],
    ["hier", "hier"],
    ["helfen/Hilfe", "hilfe"],
    ["hören", "horen"],
    ["Hunger", "hunger"],
    ["ich/mir/mich", "ich-mir-mich"],
    ["immer", "immer"],
    ["in/im", "in-im"],
    ["Ja", "ja"],
    ["jetzt", "jetzt"],
    ["Junge", "junge"],
    ["Kaffee", "kaffee"],
    ["kalt", "kalt"],
    ["kaputt", "kaputt"],
    ["Käse", "kase"],
    ["kaufen", "kaufen"],
    ["kein/e", "kein-e"],
    ["keine Ahnung", "keine-ahnung"],
    ["Kindergarten", "kindergarten"],
    ["Kino", "kino"],
    ["klein", "klein"],
    ["kommen", "kommen"],
    ["können", "konnen"],
    ["krank", "krank"],
    ["krass", "krass"],
    ["lachen", "lachen"],
    ["lang", "lang"],
    ["langsam", "langsam"],
    ["langweilig", "langweilig"],
    ["laut", "laut"],
    ["lecker", "lecker"],
    ["legen", "legen"],
    ["lernen", "lernen"],
    ["lesen", "lesen"],
    ["lieben/Liebe", "lieben-liebe"],
    ["machen", "machen"],
    ["Mädchen", "madchen"],
    ["mal", "mal"],
    ["Mama", "mama"],
    ["man", "man"],
    ["Mann", "mann"],
    ["mehr", "mehr"],
    ["mein/e", "mein-e"],
    ["Milch", "milch"],
    ["mit", "mit"],
    ["Mittwoch", "mittwoch"],
    ["mögen/möchten", "mogen"],
    ["Montag", "montag"],
    ["morgen", "morgen"],
    ["Morgen(s)", "morgen-s"],
    ["müde", "mude"],
    ["müssen", "mussen"],
    ["nach Hause", "nach-hause"],
    ["nehmen", "nehmen"],
    ["Nein", "nein"],
    ["neu", "neu"],
    ["nicht", "nicht"],
    ["nicht verstanden", "nicht-verstanden"],
    ["noch", "noch"],
    ["noch mal", "noch-mal"],
    ["Nudeln", "nudeln"],
    ["nur", "nur"],
    ["Obst", "obst"],
    ["oder", "oder"],
    ["oft", "oft"],
    ["OK", "ok"],
    ["Oma", "oma"],
    ["Opa", "opa"],
    ["Papa", "papa"],
    ["Pause", "pause"],
    ["pupsen", "pupsen"],
    ["Quatsch", "quatsch"],
    ["rechnen", "rechnen"],
    ["Regen", "regen"],
    ["richtig", "richtig"],
    ["ruhig", "ruhig"],
    ["Saft", "saft"],
    ["sagen", "sagen"],
    ["Samstag", "samstag"],
    ["satt", "satt"],
    ["sauber", "sauber"],
    ["Scheiße", "scheisse"],
    ["schlafen", "schlafen"],
    ["schlau", "schlau"],
    ["schlecht", "schlecht"],
    ["schmutzig", "schmutzig"],
    ["Schnee", "schnee"],
    ["schnell", "schnell"],
    ["schon", "schon"],
    ["schön", "schon"],
    ["Schule", "schule"],
    ["schwach", "schwach"],
    ["Schwester", "schwester"],
    ["schwimmen", "schwimmen"],
    ["sein/bin/ist/sind", "sein-bin-ist-sind"],
    ["sein/e", "sein-e"],
    ["selber", "selbst"],
    ["sie/ihr", "sie-ihr"],
    ["singen", "singen"],
    ["sitzen", "sitzen"],
    ["so", "so"],
    ["sollen", "sollen"],
    ["Sommer", "sommer"],
    ["Sonne", "sonne"],
    ["Sonntag", "sonntag"],
    ["spielen", "spielen"],
    ["Stadt", "stadt"],
    ["stark", "stark"],
    ["Stopp", "stopp"],
    ["streiten", "streiten"],
    ["Süßigkeit", "sussigkeit"],
    ["Tee", "tee"],
    ["Toilette", "toilette"],
    ["traurig", "traurig"],
    ["trinken", "trinken"],
    ["Tschüss", "tschuss"],
    ["und", "und"],
    ["Urlaub/Ferien", "urlaub-ferien"],
    ["Uups", "uups"],
    ["vergessen", "vergessen"],
    ["viel/e", "viel-e"],
    ["voll", "voll"],
    ["von/vom", "von-vom"],
    ["wach", "wach"],
    ["wann", "wann"],
    ["war/waren", "war-waren"],
    ["warten", "warten"],
    ["warum", "warum"],
    ["was", "was"],
    ["(Hände) waschen", "waschen-hande"],
    ["Wasser", "wasser"],
    ["weg", "weg"],
    ["wehtun (Schmerzen)", "schmerzen"],
    ["weil", "weil"],
    ["welche/r", "welche-r"],
    ["wenig/bisschen", "wenig-bisschen"],
    ["wenn", "wenn"],
    ["wer/wem", "wer-wem"],
    ["wichtig", "wichtig"],
    ["wie", "wie"],
    ["wieder", "wieder"],
    ["Winter", "winter"],
    ["wir/uns", "wir-uns"],
    ["wissen", "wissen"],
    ["wo", "wo"],
    ["wollen", "wollen"],
    ["Wurst", "wurst"],
    ["wütend", "wutend"],
    ["zu/zum", "zu-zum"],
    ["zusammen", "zusammen"],
  ],
  "Halloween": [
    ["Halloween", "halloween"],
    ["Süßes", "suss"],
    ["oder", "oder-variante"],
    ["Saures", "sauer-geschmack"],
    ["klingeln", "klingeln"],
    ["Süßigkeiten", "sussigkeit"],
    ["Tasche", "tragen-tasche"],
    ["danke", "danke"],
    ["Kürbis", "kurbis"],
    ["Geist", "geist"],
    ["Hexe", "hexe"],
    ["Zauberer", "zauberer"],
    ["zaubern", "zaubern"],
    ["Fledermaus", "fledermaus"],
    ["Spinne", "spinne"],
    ["Monster", "monster"],
    ["verkleiden", "kostum"],
    ["Kostüm", "kostum"],
    ["schminken", "karneval-fasching"],
    ["erschrecken", "erschrecken"],
    ["Nacht", "nachts"],
    ["Feuer", "lagerfeuer"],
    ["Show", "theater"],
    ["ansehen", "gucken-sehen-mg"],
  ],
};

await (async () => {
  const token = localStorage.getItem("feathers-jwt");
  if (!token) throw new Error("Nicht angemeldet: erst auf sign-digital.de anmelden.");
  const owner = JSON.parse(atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/"))).sub;
  const headers = { Authorization: token, Accept: "application/json", "Content-Type": "application/json" };
  const get = async (path) => {
    const r = await fetch(path, { headers });
    if (!r.ok) throw new Error(`${r.status} für ${path}`);
    return r.json();
  };

  const mine = await get(`/api/lists?owner=${owner}&ownerType=users&$limit=200`);
  const report = {};
  for (const [name, words] of Object.entries(SETS)) {
    const list = mine.data.find((l) => l.name === name);
    const ids = [];
    const missing = [];
    const twice = [];
    for (const [label, slug] of words) {
      const page = await get(`/api/signs?slug=${encodeURIComponent(slug)}`);
      const sign = page.data && page.data[0];
      if (!sign) {
        missing.push(`${label} = ${slug}`);
        continue;
      }
      if (ids.includes(sign._id)) {
        twice.push(`${label} = ${slug} (${sign.name})`);
        continue;
      }
      ids.push(sign._id);
    }
    report[name] = {
      liste: list ? `gefunden (${list.signs.length} Gebärden darin)` : "FEHLT: erst leer anlegen",
      gebärden: ids.length,
      "nicht gefunden": missing,
      "doppelt, nur einmal": twice,
    };
    if (DRY || !list) continue;
    if (list.signs.length > 0) {
      report[name].liste = "nicht leer: nichts geändert";
      continue;
    }
    // The site's own add sends the whole list back with signs as ids; so does this.
    const body = { ...list, signs: ids, learnedSigns: list.learnedSigns || [] };
    const r = await fetch(`/api/lists/${list._id}`, { method: "PATCH", headers, body: JSON.stringify(body) });
    report[name].liste = r.ok ? `gefüllt mit ${ids.length} Gebärden` : `PATCH ${r.status}: nichts geändert`;
  }
  console.log(DRY ? "Probelauf, nichts geändert:" : "Umzug:", JSON.stringify(report, null, 2));
  return report;
})();
