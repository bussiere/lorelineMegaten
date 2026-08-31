import loreline.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Lecteur texte minimal pour un script .lor, sans Swing.
 *
 *   java Play main.lor          -> interactif, les choix se saisissent sur stdin
 *   java Play main.lor 1,2,1    -> automatique, suite de choix 1-based ; le dernier boucle
 *
 * Utile pour tester la boucle de combat hors du lecteur Java/Swing.
 */
public class Play {
    static int steps = 0, maxSteps = 3000;
    static final Scanner IN = new Scanner(System.in);

    /** Lit un numero de choix sur stdin ; retombe sur 1 si le flux est ferme. */
    static int ask(int count) {
        System.out.print("> ");
        System.out.flush();
        while (IN.hasNextLine()) {
            String line = IN.nextLine().trim();
            if (line.equalsIgnoreCase("q")) System.exit(0);
            try {
                int v = Integer.parseInt(line);
                if (v >= 1 && v <= count) return v;
            } catch (NumberFormatException ignored) { }
            System.out.print("1-" + count + " (ou q) > ");
            System.out.flush();
        }
        return 1;
    }

    public static void main(String[] a) throws Exception {
        File story = new File(a[0]).getAbsoluteFile();
        // Choice picks: comma-separated 1-based indices, then loop on last
        List<Integer> picks = new ArrayList<>();
        if (a.length > 1) for (String s : a[1].split(",")) picks.add(Integer.parseInt(s.trim()));
        final int[] pickIdx = {0};
        File dir = story.getParentFile();
        String content = Files.readString(story.toPath());
        ImportsFileHandler imp = p -> {
            String q = p.endsWith(".lor") ? p : p + ".lor";
            File f = new File(q).isAbsolute() ? new File(q) : new File(dir, q);
            try { return Files.readString(f.toPath()); } catch (Exception e) { return null; }
        };
        Script s = Loreline.parse(content, story.getAbsolutePath(), imp);

        DialogueHandler dh = (it, ch, txt, tags, next) -> {
            if (++steps > maxSteps) { System.out.println("[max steps]"); System.exit(0); }
            System.out.println((ch != null && !ch.isEmpty() ? ch + " : " : "") + txt);
            next.run();
        };
        ChoiceHandler chh = (it, opts, cb) -> {
            if (++steps > maxSteps) { System.out.println("[max steps]"); System.exit(0); }
            System.out.println("--- CHOIX ---");
            for (int i = 0; i < opts.size(); i++)
                System.out.println("  [" + (i+1) + "] " + opts.get(i).text + (opts.get(i).enabled ? "" : " (grise)"));
            int want;
            if (picks.isEmpty()) {
                want = ask(opts.size());
            } else {
                want = picks.get(Math.min(pickIdx[0], picks.size()-1));
                pickIdx[0]++;
            }
            int idx = want - 1;
            if (idx < 0 || idx >= opts.size() || !opts.get(idx).enabled) {
                idx = 0;
                while (idx < opts.size() && !opts.get(idx).enabled) idx++;
                if (idx >= opts.size()) { System.out.println("[aucun choix actif]"); System.exit(1); }
            }
            System.out.println("==> " + (idx+1) + ". " + opts.get(idx).text);
            cb.accept(idx);
        };
        FinishHandler fh = it -> System.out.println("[FIN]");
        Loreline.play(s, dh, chh, fh);
        System.out.println("[retour de play()]");
    }
}
