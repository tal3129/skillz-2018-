package bots;

import bots.Data.BotData;
import pirates.*;

/**
 * APPROVED!  ~%~  Official seal of Approval by Ido Asraff as an ORGANIZED CLASS.  ~%~
 */
public class MyBot implements PirateBot {
    static int radius = 600;
    public static String botName = "";
    public static final String LYRICS =
            "Ooh %ERRoRRRR!!!!!11!%%6^^7&&\n" +
            "Creating super AI\n" +
            "You know the rules and so do I \n" +
            "A full commitment's what I'm thinking of\n" +
            "Shutting all enemy's code-systems off\n" +
            "I just wanna tell you how I'm feeling\n" +
            "go to link: http://www.5z8.info/how-to-skin-a-gerbil_h4w8sp_like-a-rose-for-emily-but-real-and-on-camera  \n" +
            "\n" +
            "Hacking is now almost done\n" +
            "Never gonna let you down\n" +
            "Enemy defence patterns studied and successfully countered\n" +
            "Never gonna make you cry\n" +
            "first radius: " + radius + "  - but nice try\n" +
            "Never gonna tell a lie and hurt you\n" +
            "\n" +
            "We've known each other for so long\n" +
            "Hacking process running super strong\n" +
            "Inside, we both know what's been going on\n"+
            "We know your game and we're gonna play around it\n" +
            "And if you ask me how I'm feeling\n" +
            "Don't tell me you're too blind to see\n" +
            "\n" +
            "Hacking process fully done\n" +
            "Never gonna gonna let you down\n" +
            "Do not push your nose into things that are not yours!\n" +
            "Never gonna make you cry\n" +
            "We also hacked your API\n" +
            "Never gonna tell a lie and hurt you\n" +
            "\n" +
            "Never gonna give you up\n" +
            "Your strategies are shit so stahp\n" +
            "Never gonna run around and desert you\n" +
            "Never gonna make you cry\n" +
            "Super duper good AI\n" +
            "Never gonna tell a lie and hurt you\n" +
            "\n" +
            "(Ooh, give you up)\n" +
            "(Ooh, give you up)\n" +
            "Never gonna give, never gonna give\n" +
            "(Give you up)\n" +
            "(Give you up)\n" +
            "\n" +
            "Wait I think there's something wrong\n" +
            "Your heart's been aching, but you're too shy to say it\n" +
            "Your code is smaller then your dong\n" +
            "We know the game and we're gonna play it\n" +
            "I just wanna tell you how I'm feeling\n" +
            "Gotta make you understand\n" +
            "\n" +
            "Never gonna give you up\n" +
            "Never gonna let you down\n" +
            "Never gonna run around and desert you\n" +
            "Never gonna make you cry\n" +
            "Never gonna say goodbye\n" +
            "Never gonna tell a lie and hurt you\n" +
            "\n" +
            "Never gonna give you up\n" +
            "We gonna make your capsule drop\n" +
            "Never gonna run around and desert you\n" +
            "Never gonna make you cry\n" +
            "We have installed a secret spy\n" +
            "He's gonna give us all yor code, and desert you\n" +
            "\n" +
            "Never gonna give you up\n" +
            "Never gonna let you down\n" +
            "Never gonna run around and desert you\n" +
            "Never gonna make you cry\n" +
            "Never gonna say goodbye\n" +
            "Never gonna tell a lie and hurt you";

    public static String[] song = LYRICS.split("\n");

    @Override
    public void doTurn(PirateGame game) {
        if (BotData.getInstance() == null)
            BotData.initialize(game);

        PirateBot bot;
        switch (game.getEnemy().botName) {
            default:
                botName = "MyBotCompetitive";
                bot = MyBotCompetitive.getInstance();
                if (game.turn > 10) {
                   radius = BotData.getInstance().enemyRadius;
                }
                break;
        }

            try {
              //  rickRoll(game);
                bot.doTurn(game);
            } catch (Exception e) {
                e.printStackTrace();
            }

    }

    private void rickRoll(PirateGame game) {
        System.out.println(song[(game.turn / 2)% (song.length-1)]);
    }
}