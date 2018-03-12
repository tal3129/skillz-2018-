package bots.goodOlBot.Data;

import bots.goodOlBot.*;
import bots.goodOlBot.Actions.Sails;
import bots.goodOlBot.Predictions.PredictionManager;
import bots.goodOlBot.Types.Unit;
import bots.goodOlBot.Types.UpgradedPirate;
import pirates.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;


/**
 * holds the bot's data - his pirate squads, asteroid and enemy info, etc.
 * <p>
 * APPROVED!  ~%~  Official seal of Approval by Ido Asraff as a DISGUSTING CLASS.  ~%~
 */

public class BotData {
    private static final int UNIT_SIZE = 3;
    public static final int MIN_DISTANCE_FROM_MS_STICKING = 1500;
    public static final int ENEMY_DEFENDERS_FOR_TURNS = 1;
    private static int minePortalPushersAmount = 0;
    private static int basePortalPushersAmount = 0;
    public int DEFENDER_AMOUNT = 0;
    public static int CAREFUL_RELOAD_TURNS = 5;

    public final int ATTACKERS_TO_EACH_CAPSULE = 1;

    private static BotData instance;
    public static PirateGame game;

    public final int PORTAL_DEFENDING_PUSHER_AMOUNT = 1;
    public final int PORTAL_ATTACKING_PUSHER_AMOUNT = 3;
    public final int DEVIATION_LOCATIONS = 25;

    public final int FAR_ENOUGH_TO_KILL_OTHERS_RADIUS = 2750;
    public final int SAME_RADIUS_TURNS = 1;
    public final int MY_MIN_BIG_RADIUS = 1200;
    public final int MY_MAX_BIG_RADIUS = 1200;
    public int attackersDefendingRadius;
    public final int MY_MIN_SMALL_RADIUS = 600;
    public final int MY_MAX_SMALL_RADIUS = 600;

    public int SMALL_RADIUS = 600;
    public int BIG_RADIUS = 1200;
    public int SMALL_CHASE_R = SMALL_RADIUS + 400;
    public int BIG_CHASE_R = BIG_RADIUS + 400;

    public int CAREFUL_DISTANCE = 0; //careful distance to get around enemies and asteroids
    public int MAX_BOMB_COUNTDOWN_SAFE = 2;
    public int CAREFUL_PUSH_DISTANCE = 60; //careful distance to get around enemies and asteroids

    public Location[] myMines; //list of my mines
    public Location[] enemyMines; //list of enemy mines
    public List<Integer> asteroidsPushedIds; //ids of asteroids we already pushed this turn
    public List<UpgradedPirate> upgradedEnemies;
    public List<UpgradedPirate> upgradedPirates;

    public List<Unit> units; //include 2 pirates, one of them has capsule, the pirates in the unit act togehther
    public List<Pirate> defendingPirates; //the defending team, blocking enemies from reaching their base
    public List<Pirate> attackingPirates; //the attacking team with the carriear and its helpers, might be divided
    public List<Pirate> minePortalPushers; // pirates that push a portal to between our mines
    public List<Pirate> basePortalPushers;
    public List<Pirate> portalRemovers; //

    public int[] amountOfAttackersGoingToCapsule;

    public int enemyRadius = 0;
    public int enemySecondRadius = 0;

    public Asteroid astroOnOurBase = null;

    public final int PORTAL_CLOSE_ENOUGH_TO_TARGET_DISTANCE = 50;
    public int basePortalInRightPlaceCount = 0;
    public int minesPortalInRightPlaceCount = 0;
    public PredictionManager predictionManager;
    public List<Location> snakeLocations;


    private BotData(PirateGame game) {

        this.game = game;

        // the array that counts pushes made in a turn, so a pirate won't push out a pirate that would already be pushed out


        // the defending pirate arrayList
        defendingPirates = new ArrayList<>();
        attackingPirates = new ArrayList<>();
        minePortalPushers = new ArrayList<>();
        basePortalPushers = new ArrayList<>();
        portalRemovers = new ArrayList<>();

        myMines = new Location[game.getMyCapsules().length];
        for (int i = 0; i < myMines.length; i++)
            myMines[i] = game.getMyCapsules()[i].location;

        enemyMines = new Location[game.getEnemyCapsules().length];
        for (int i = 0; i < enemyMines.length; i++)
            enemyMines[i] = game.getEnemyCapsules()[i].location;


        upgradedPirates = new ArrayList<>();
        for (Pirate p : game.getAllMyPirates())
            upgradedPirates.add(new UpgradedPirate(p));

        upgradedEnemies = new ArrayList<>();
        for (Pirate p : game.getAllEnemyPirates())
            upgradedEnemies.add(new UpgradedPirate(p));


        if (game.getMyMotherships().length > 1) {
            DEFENDER_AMOUNT = 0;
            minePortalPushersAmount = 3;
            basePortalPushersAmount = 2;
        }
        predictionManager = PredictionManager.getInstance();

    }

    public static void initialize(PirateGame game) {
        instance = new BotData(game);
    }

    public static BotData getInstance() {
        return instance;
    }

    public void update(PirateGame newGame) { //initialize all data
        this.game = newGame;

        initializeFlags();

        initializeSnakeLocations();

        updatePirateInfo();

        updateArraysAndLists();

        //    initializeAttackingPirates(); //initializing all pirates as attackers, but if any other role is lacking, it takes from here

        //initializeDefendingPirates();

        initializeBasePortalPushers();

        initializeMinePortalPushers(); //takes


        // base portal pushers, if there aren't enough - the units first try to fill with attackers , if not enough - mine portal pushers
        initializeUnits();


        swapStates();

        detectShittyTactics();

        predictionManager.makePredictions();
    }

    private void initializeSnakeLocations() {
        snakeLocations = new ArrayList<>();
    }

    private void initializeFlags() {
        Flags.ASTEROID_ON_BASE = false;
        Flags.SWAPED_THIS_TURN = false;
    }

    private void updatePirateInfo() {
        for (int i = 0; i < upgradedEnemies.size(); i++)
            upgradedEnemies.get(i).setPirate(game.getAllEnemyPirates()[i]);

        for (int i = 0; i < upgradedPirates.size(); i++)
            upgradedPirates.get(i).setPirate(game.getAllMyPirates()[i]);

        predictionManager.updateReliability();

        // update enemies - updates next and last locations
        for (UpgradedPirate umo : getEnemyUPs())
            umo.updateDirectionAndLoc();
        for (UpgradedPirate up : getMyLivingPirates())
            up.updateDirectionAndLoc();

    }

    private void updateArraysAndLists() {
        // reset the list of asteroids that was pushed this turn
        asteroidsPushedIds = new ArrayList<>();

        amountOfAttackersGoingToCapsule = new int[MapMethods.getAvalibleCapsules().size() + 4];
        for (int i = 0; i < amountOfAttackersGoingToCapsule.length; i++)
            amountOfAttackersGoingToCapsule[i] = 0;

        minePortalPushers.clear();
        List<Pirate> piratesWithNoCapsule = Arrays.stream(game.getMyLivingPirates()).filter(pirate -> !pirate.hasCapsule()).collect(Collectors.toList());


        if (game.getEnemyMotherships().length == 1) // we are only defending 1 mothership
            defendingPirates = MapMethods.closestToObjectList(game.getEnemyMotherships()[0], piratesWithNoCapsule, DEFENDER_AMOUNT);


        // placing the fatties in the back
        List<Pirate> normalPirates = defendingPirates.stream().filter(pirate -> pirate.stateName.equals("normal")).collect(Collectors.toList());
        defendingPirates.removeAll(normalPirates);
        defendingPirates.addAll(normalPirates);

        // all the pirates that are not defending are attacking
        attackingPirates = Arrays.stream(game.getMyLivingPirates()).filter(pirate -> !defendingPirates.contains(pirate)).collect(Collectors.toList());
    }


    private void initializeAttackingPirates() {
        // all the pirates that are not defending are attacking
        attackingPirates.clear();
        attackingPirates.addAll(Arrays.asList(game.getMyLivingPirates()));
    }

    private void initializeBasePortalPushers() {
        if (game.getAllWormholes().length > 0) {
            Location myMothership = game.getMyMotherships()[0].location;
            Wormhole closeHole = MapMethods.closest(myMothership, game.getAllWormholes());
            basePortalPushers = MapMethods.closestToObjectList(closeHole, attackingPirates.stream().filter(pirate -> !pirate.hasCapsule()).collect(Collectors.toList()), basePortalPushersAmount);
        }
        attackingPirates.removeAll(basePortalPushers);
    }


    private void initializeMinePortalPushers() {
        if (game.getAllWormholes().length > 0) {
            Location avgOfMyMines = MapMethods.getAvarageLoc(myMines);
            Wormhole closeHole = MapMethods.closest(avgOfMyMines, game.getAllWormholes());
            minePortalPushers = MapMethods.closestToObjectList(closeHole, attackingPirates.stream().filter(pirate -> !pirate.hasCapsule()).collect(Collectors.toList()), minePortalPushersAmount);
        }

        attackingPirates.removeAll(minePortalPushers);
    }


    public void initializeUnits() {

        units = new ArrayList<>();
        List<Pirate> carryBoys = new ArrayList<>();
        Iterator<Pirate> pirateIterator = attackingPirates.iterator();
        Pirate p;

        // create 1 unit for each friendly carry-boi
        while (pirateIterator.hasNext()) {
            p = pirateIterator.next();
            if (p.hasCapsule()) {
                Unit current = new Unit(p);
                units.add(current);
                if (!current.offside)
                    carryBoys.add(p);
                pirateIterator.remove();
            }
        }

        List<Pirate> currentCbs = new ArrayList<>(carryBoys);
        Pirate cb = null;
        pirateIterator = attackingPirates.iterator();
        boolean finishedAttackers = false;
        boolean finishedBasePushers = false;
        List<Pirate> currentIterated = attackingPirates;
        while (!(finishedAttackers && finishedBasePushers) && currentCbs.size() > 0) {
            if (!pirateIterator.hasNext()) {
                if (finishedAttackers) { // if I finished the first 2 list
                    finishedBasePushers = true;
                    pirateIterator = minePortalPushers.iterator();
                    currentIterated = minePortalPushers;
                } else {
                    finishedAttackers = true;
                    pirateIterator = basePortalPushers.iterator();
                    currentIterated = basePortalPushers;
                }
            } else {
                p = pirateIterator.next();
                // join with carry boy you are the closest to him and vice versa
                for (Pirate carryBoy : currentCbs) {
                    if (MapMethods.closest(carryBoy, currentIterated).equals(p)) {
                        cb = MapMethods.closest(p, currentCbs);
                    }
                }
                if (cb != null && getUnit(cb).size() < 2) {
                    getUnit(cb).addPirate(p);
                    currentCbs.remove(cb);
                    pirateIterator.remove();
                }
            }
        }
    }
/*
        // add the second pirates to the units
        currentCbs = new ArrayList<>(carryBoys);
        cb = null;
        pirateIterator = attackingPirates.iterator();
        finishedAttackers = false;
        finishedBasePushers = false;
        currentIterated = attackingPirates;
        while (!(finishedAttackers && finishedBasePushers) && currentCbs.size() > 0) {
            if (!pirateIterator.hasNext()) {
                if (finishedAttackers) { // if I finished the first 2 list
                    finishedBasePushers = true;
                    pirateIterator = minePortalPushers.iterator();
                    currentIterated = minePortalPushers;
                } else {
                    finishedAttackers = true;
                    pirateIterator = basePortalPushers.iterator();
                    currentIterated = basePortalPushers;
                }
            } else {
                p = pirateIterator.next();
                // join with carry boy you are the closest to him and vice versa
                for (Pirate carryBoy : currentCbs) {
                    if (MapMethods.closest(carryBoy, currentIterated).equals(p)) {
                        cb = MapMethods.closest(p, currentCbs);
                    }
                }
                if (cb != null && getUnit(cb).size() < 3) {
                    getUnit(cb).addPirate(p);
                    currentCbs.remove(cb);
                    pirateIterator.remove();
                }
            }
        }
    }*/


    private void swapStates() { //called at the start of the turn
        //   trySwapToCatchOffside();

        // replace a heavy ATTACKER with a normal defender
        replaceHeavyAttackerWithNormalDefender();

        // replace a heavy UNIT member with a defender if the member can't push and he doesn't have a capsule
        replaceHeavyUnitMemberWithNormalDefender();

        //if leader is fat, and he is not threatened by 2 this turn or the next (assume going to his closest mothership) then turn him to be skinny again
        leaderInOffsideGetNormal();

    }


    private void detectShittyTactics() {
        //if the asteroid is resting on one of our motherships, they are using shitty tactics
        for (Mothership mothership : game.getMyMotherships())
            for (Asteroid asteroid : game.getLivingAsteroids())
                if (asteroid.distance(mothership) < asteroid.size && asteroid.direction.equals(new Location(0, 0))) {
                    Flags.ASTEROID_ON_BASE = true;
                    astroOnOurBase = asteroid;
                }
    }

    public Unit getUnit(Pirate pirate) {
        for (Unit unit : units) {
            if (unit.contains(pirate))
                return unit;
        }
        return null;
    }

    private List<UpgradedPirate> getEnemyUPs() {
        return upgradedEnemies.stream().filter(upgradedPirate -> upgradedPirate.pirate.isAlive()).collect(Collectors.toList());
    }

    public List<UpgradedPirate> getMyLivingPirates() {
        return upgradedPirates.stream().filter(upgradedPirate -> upgradedPirate.pirate.isAlive()).collect(Collectors.toList());
    }


    public <T> int indexOf(T[] arr, T n) {
        for (int i = 0; i < arr.length; i++)
            if (n == arr[i])
                return i;
        return -1;
    }

    //receives a pirate, returns him upgraded
    public UpgradedPirate upgraded(Pirate pirate) {
        for (UpgradedPirate upgradedPirate : upgradedEnemies)
            if (upgradedPirate.pirate.equals(pirate))
                return upgradedPirate;
        for (UpgradedPirate upgradedPirate : upgradedPirates)
            if (upgradedPirate.pirate.equals(pirate))
                return upgradedPirate;
        return null;
    }

    public void setGame(PirateGame game) {
        this.game = game;
    }

    public int max(List<Integer> arr) {
        int max = Integer.MIN_VALUE;
        for (int a : arr)
            max = a > max ? a : max;
        return max;
    }

    public int min(List<Integer> arr) {
        int min = Integer.MAX_VALUE;
        for (int a : arr)
            min = a < min ? a : min;
        return min;
    }


    // this method receives an object and whether it is ours and returns if it is offside
    public boolean isOffside(MapObject checkObj, boolean ourObject) {
        Mothership closestMs = MapMethods.closest(checkObj, ourObject ? game.getMyMotherships() : game.getEnemyMotherships());
        return MapMethods.canGoStraightAgainstSmartDefenseCareFully(checkObj, ourObject ? game.getEnemyLivingPirates() : game.getMyLivingPirates(), closestMs, 0);
    }

    public boolean isOffsideWithForTurns(MapObject checkObj, boolean ourObject, int forTurns) {
        Mothership closestMs = MapMethods.closest(checkObj, ourObject ? game.getMyMotherships() : game.getEnemyMotherships());
        return MapMethods.canGoStraightAgainstSmartDefenseCareFully(checkObj, ourObject ? game.getEnemyLivingPirates() : game.getMyLivingPirates(), closestMs, forTurns);
    }

    public boolean isOffside(Pirate pirate) {
        boolean ourObject = pirate.owner.equals(game.getMyself()); //2deep4me
        Mothership closestMs = MapMethods.closest(pirate, ourObject ? game.getMyMotherships() : game.getEnemyMotherships());
        return MapMethods.canGoStraightAgainstSmartDefenseCareFully(pirate, ourObject ? game.getEnemyLivingPirates() : game.getMyLivingPirates(), closestMs, 0);
    }

    public boolean isOffsideForDefenseThatDoesntSwap(Pirate pirate) {
        boolean ourObject = pirate.owner.equals(game.getMyself()); //2deep4me
        Mothership closestMs = MapMethods.closest(pirate, ourObject ? game.getMyMotherships() : game.getEnemyMotherships());
        return MapMethods.canGoStraightAgainstStupidDefenseCareFully(pirate, ourObject ? game.getEnemyLivingPirates() : game.getMyLivingPirates(), closestMs, 0);
    }


    private void leaderInOffsideGetNormal() {
        Iterator<Pirate> defendingPirateIterator;
        Pirate currentDefender;
        for (Unit unit : units) {
            defendingPirateIterator = attackingPirates.iterator();
            while (defendingPirateIterator.hasNext()) {
                currentDefender = defendingPirateIterator.next();
                //if the unit member is heavy, is not the leader and he can't push, make him normal
                if (unit.leader.stateName.equals("heavy")) // if the unit leader is heavy
                    if (Sails.piratesThreatsOnLoc(unit.leader.location.towards(MapMethods.closest(unit.leader, game.getMyMotherships()), unit.leader.maxSpeed), game.getEnemyLivingPirates(), 200, 0) < 2) // if the leader won't be threatened by 2
                        if (currentDefender.stateName.equals("normal") && !Flags.SWAPED_THIS_TURN) {
                            currentDefender.swapStates(unit.leader);
                            System.out.println(currentDefender + " swaps with " + unit.leader + " to make leader in offside get normal");
                            Flags.SWAPED_THIS_TURN = true;
                            defendingPirateIterator.remove(); // remove the defender because he made his turn
                        }
            }
        }
    }

    private void replaceHeavyAttackerWithNormalDefender() {
        Iterator<Pirate> pirateIterator = attackingPirates.iterator();
        Pirate p;
        while (pirateIterator.hasNext()) {
            p = pirateIterator.next();
            if (p.stateName.equals("heavy"))
                for (Pirate defendingPirate : defendingPirates) {
                    if (defendingPirate.stateName.equals("normal") && !Flags.SWAPED_THIS_TURN) {
                        p.swapStates(defendingPirate);
                        System.out.println(p + " swaps with " + defendingPirate + " to replace Heavy Attacker With Normal Defender");
                        Flags.SWAPED_THIS_TURN = true; // there can be only one swap each turn
                        pirateIterator.remove(); // remove the attacker because he made his turn
                    }
                }
        }
    }

    private void replaceHeavyUnitMemberWithNormalDefender() {
        Iterator<Pirate> unitPiratesIterator;
        Pirate current;
        for (Unit unit : units) {
            unitPiratesIterator = unit.pirates.iterator();
            while (unitPiratesIterator.hasNext()) {
                current = unitPiratesIterator.next();
                //if the unit member is heavy, is not the leader and he can't push, make him normal
                if (current.stateName.equals("heavy") && current.capsule == null && current.pushReloadTurns > 3)
                    for (Pirate defendingPirate : defendingPirates) {
                        if (defendingPirate.stateName.equals("normal") && !Flags.SWAPED_THIS_TURN) {
                            current.swapStates(defendingPirate);
                            System.out.println(current + " swaps with " + defendingPirate + " to replace Heavy Unit Member With Normal Defender");
                            Flags.SWAPED_THIS_TURN = true; // there can be only one swap each turn
                            unitPiratesIterator.remove(); // remove the unit member because he made his turn
                        }
                    }
            }
        }


    }

    private void trySwapToCatchOffside() {
        //if we can stop an enemy from being offside but only if we get thin then get thin
        Pirate closestEnemy = MapMethods.closest(game.getEnemyMotherships()[0], Arrays.stream(game.getEnemyLivingPirates()).filter(pirate -> pirate.capsule != null).collect(Collectors.toList()));
        boolean inOffside = closestEnemy != null && isOffsideForDefenseThatDoesntSwap(closestEnemy);

        int originalSpd;


        if (inOffside && !Flags.SWAPED_THIS_TURN) {
            Pirate swapper = null;
            for (Pirate defender : defendingPirates.stream().filter(pirate1 -> pirate1.stateName.equals("heavy")).collect(Collectors.toList())) {  //for each hea pirate. / Out of context / how2lambda: pirate.blaballbabalbablabalbalb-->balba<--lbalbalbalbalbalbalba.collect.collctions().collect().annoyingshit().---><-----ooohaaah.Collect.Collections();
                originalSpd = defender.maxSpeed;
                defender.maxSpeed = game.pirateMaxSpeed;

                if (!BotData.getInstance().isOffsideForDefenseThatDoesntSwap(closestEnemy)) {  // if an enemy was offside before and now he isn't, find someone to swap with

                    for (Unit unit : units)
                        for (Pirate pirate : unit.pirates.stream().filter(pirate -> pirate != unit.leader).collect(Collectors.toList()))
                            if (pirate.stateName.equals("normal"))
                                swapper = pirate;

                    if (swapper == null)  // if we didn't find a swapper in the units, seek him the attackers. /Out Of Context / pIraTE.iSOfFSiDe
                        for (Pirate attacker : attackingPirates)
                            if (attacker.stateName.equals("normal"))
                                swapper = attacker;


                }
                if (swapper != null) {
                    swapper.swapStates(defender);
                    System.out.println(swapper + " swaps with " + defender);
                    System.out.println("to catch an enemy (defender)");
                    Flags.SWAPED_THIS_TURN = true; // there can be only one swap each turn
                    attackingPirates.remove(swapper);
                    if (getUnit(swapper) != null)
                        getUnit(swapper).remove(swapper);
                }

                defender.maxSpeed = originalSpd;

                if (Flags.SWAPED_THIS_TURN)
                    return;
            }
        }
    }

    public List<Location> getUnchasedMines() {
        //hehe look at me im gonna do this simple self-explainotary method in one unintelligible line
        //return Arrays.stream(myMines).filter(mine -> amountOfAttackersGoingToCapsule[BotData.getInstance().indexOf(myMines,mine)] == ATTACKERS_TO_EACH_CAPSULE).collect(Collectors.toList());

        List<Location> res = new ArrayList<>();

        for (int i = 0; i < myMines.length; i++) {
            if (!(amountOfAttackersGoingToCapsule[i] == ATTACKERS_TO_EACH_CAPSULE))
                res.add(myMines[i]);
        }

        return res;

    }

}
