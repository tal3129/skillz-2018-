package bots;

import bots.Actions.Pushings;
import bots.Actions.Sails;
import bots.Data.BotData;
import bots.Data.Flags;
import bots.Types.Unit;
import bots.Types.UpgradedPirate;
import pirates.*;

import java.util.*;

public class MyBotCompetitive implements PirateBot {

    public PirateGame game;
    private BotData data;
    private static MyBotCompetitive instance;

    @Override
    public void doTurn(PirateGame game) {

        //if this is the first turn, initialize the arrays and vars
        if (game.turn == 1)
            initializeStatics(game);

        updateVariables(game); //also create the FREAKING UNITS

        doUnitsTurn(); //the units sail and score

        doAttackingTurn(); //the attackers sail to the mines and stray

        doStickedPiratesTurn(); //sticky pirates try to go to valueloc or score  point if they're a carry boi

        printData(); // prints how many pirates are in which role

        findEnemyRadius();
    }


    public void printData() {
        int count = 0;
        for (Unit unit : data.units)
            count += unit.pirates.size();

        System.out.println("attackers: " + data.attackingPirates.size());

        System.out.println("in unit: " + count);

        System.out.println("in stickies: " + data.piratesWithSticky.size());

        System.out.println("total: " + game.getMyLivingPirates().length + ", in roles: " + (count + data.attackingPirates.size() + data.piratesWithSticky.size()));
        //     System.out.println(data.predictionManager);
    }

    //the defence is in 2 lines, 2 pirates each. the defence only pushes the capsule or an asteroid

    private void doUnitsTurn() {
        for (Unit unit : data.units) /* -> */ doUnitTurn(unit);
    }

    private void doUnitTurn(Unit unit) {
        if (!unit.tryPush()) { //try to push, if we can then that's what we do this turn.
            if (unit.getLeader() != null) {
                if (Flags.ASTEROID_ON_BASE)
                    //sail to base but not into it because asteroid
                    unit.carefulSail(MapMethods.getShorterRoute
                            (unit.getLeader(), data.astroOnOurBase.location
                                    .towards(unit.getLeader().location, game.asteroidSize + 10)));
                else
                    unit.carefulSail(MapMethods.getShorterRoute(unit.getLeader(), MapMethods.closest(unit.getLeader(), game.getMyMotherships()))); //sailing to closest mother ship using short route
            }
        }
    }

    private void doAttackingTurn() {
        //find the best attacking pirate to hit portal
        List<Pirate> attackingDidntPush = new ArrayList<>(data.attackingPirates);

        int strayCount = 0;
        for (Pirate pirate : data.attackingPirates)
            if ((Pushings.tryPush(pirate)))
                attackingDidntPush.remove(pirate); //pirate has pushed

        for (Pirate attacker : attackingDidntPush) {
            Location towardsClosestMine = Sails.getNormalCarefulSailLoc(attacker, MapMethods.getShorterRoute(attacker, MapMethods.closest(attacker, data.myMines)));

            Location strayLoc = null;
            int i = 1;

            while (strayLoc == null && i <= BotData.STRAY_TURNS) {
                strayLoc = MapMethods.getStrayTowardsCapsuleLoc(attacker, i);
                i++;
            }

            if (strayLoc != null && strayCount < BotData.STRAYER_AMOUNT) {
                towardsClosestMine = strayLoc;
                strayCount++;
                System.out.println("straying");
            }

            attacker.sail(towardsClosestMine);
        }
    }

    private void doStickedPiratesTurn() {

        for (Pirate sticked : data.piratesWithSticky) {

            if (Pushings.tryPush(sticked))
                continue;

            if (sticked.capsule != null) {
                if (sticked.distance(MapMethods.closest(sticked, game.getMyMotherships())) <= sticked.stickyBombs[0].countdown * (sticked.maxSpeed) + game.mothershipUnloadRange)//can reach ms
                    sticked.sail(MapMethods.closest(sticked, game.getMyMotherships())); //sail to ms
                else
                    sticked.sail(MapMethods.bestPlaceForStickyBombToGoOff(sticked.location, sticked.maxSpeed, game.stickyBombExplosionRange, sticked.stickyBombs[0].countdown)); //sail to maximize value
            } else {
                sticked.sail(MapMethods.bestPlaceForStickyBombToGoOff(sticked.location, sticked.maxSpeed, game.stickyBombExplosionRange, sticked.stickyBombs[0].countdown));
            }
        }
    }

    //**CALLED ONCE**  initializes the statics, must receive game since this is called before setting the game property
    public void initializeStatics(PirateGame game) {
        this.game = game;
        MapMethods.initializeStatics(game);
        data = BotData.getInstance();
    }

    // called every turn, updates the variables
    public void updateVariables(PirateGame game) {
        this.game = game;
        MapMethods.setGame(game);
        data.update(game);
    }

    public static MyBotCompetitive getInstance() {
        if (instance == null)
            instance = new MyBotCompetitive();
        return instance;
    }

    private Wormhole partnerDest(Pirate attacker) {
        List<Wormhole> partners = new ArrayList<>();
        for (Mothership mothership : mothershipsWithPortal())
            partners.add(MapMethods.closest(mothership, game.getAllWormholes()).partner);
        return MapMethods.closest(attacker, partners);
    }


    public List<Mothership> mothershipsWithPortal() {
        List<Mothership> res = new ArrayList<>();
        for (Mothership mothership : game.getMyMotherships()) {
            Wormhole closeHole = MapMethods.closest(mothership, game.getAllWormholes());
            if (closeHole != null &&
                    (closeHole.distance(mothership) < game.mothershipUnloadRange || data.isOffside(closeHole, true)))
                res.add(mothership);
        }
        return res;
    }

    private void findEnemyRadius() {
        for (UpgradedPirate upgradedPirate : data.upgradedEnemies)
            upgradedPirate.setLastLocation(upgradedPirate.pirate.location);
        for (UpgradedPirate upgradedPirate : data.upgradedPirates)
            upgradedPirate.setLastLocation(upgradedPirate.pirate.location);


        int maxStaysOfThePirateThatStayedInTheSamePlaceTheMost = -1; //max of the maximal staying value of each enemy
        int anotherRaduisMaxStays = -1; //the same as max stays but for another enemyRadius
        int maxIndx = 0; //index of the pirate that stayed in the same place the most
        int secondMaxIndx = 0; //index of the pirate that stayed in the same place the most but has another enemyRadius


        List<UpgradedPirate> upgradedEnemies = data.upgradedEnemies;

        for (int i = 0; i < upgradedEnemies.size(); i++) {
            UpgradedPirate current = upgradedEnemies.get(i);
            int m = data.max(current.assumedRadiuses); //the place where this pirate stayed the most
            //int indxOfMaxForPirate = current.assumedRadiuses.indexOf(m);

            if (m > maxStaysOfThePirateThatStayedInTheSamePlaceTheMost) {

                maxStaysOfThePirateThatStayedInTheSamePlaceTheMost = m;

                maxIndx = i;
            }
        }

        int placeInWhichSomeoneStayedTheMost = upgradedEnemies.get(maxIndx).assumedRadiuses.indexOf(maxStaysOfThePirateThatStayedInTheSamePlaceTheMost);

        //find the enemy that stayed in the same place the most but the place where he did it is different than placeInWhichSomeoneStayedTheMost
        for (int i = 0; i < upgradedEnemies.size(); i++) {
            UpgradedPirate current = upgradedEnemies.get(i);
            int m = data.max(current.assumedRadiuses);
            int indxOfMaxForPirate = current.assumedRadiuses.indexOf(m);

            if (m > anotherRaduisMaxStays && indxOfMaxForPirate != placeInWhichSomeoneStayedTheMost) {
                anotherRaduisMaxStays = m;
                secondMaxIndx = i;
            }
        }

        int secondPlaceInWhichSomeoneStayedTheMost = data.upgradedEnemies.get(secondMaxIndx).assumedRadiuses.indexOf(anotherRaduisMaxStays);
        data.enemyRadius = (placeInWhichSomeoneStayedTheMost * UpgradedPirate.UNIT_DISTANCE);
        data.enemySecondRadius = (secondPlaceInWhichSomeoneStayedTheMost * UpgradedPirate.UNIT_DISTANCE);
        // System.out.println("rad 1: " + data.enemyRadius);
        // System.out.println("rad 2: " + data.enemySecondRadius);


    }

}