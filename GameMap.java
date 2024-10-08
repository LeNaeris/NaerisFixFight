package soufix.area.map;

import soufix.area.SubArea;
import soufix.area.map.entity.InteractiveDoor;
import soufix.area.map.entity.MountPark;
import soufix.client.Player;
import soufix.client.other.Party;
import soufix.common.ConditionParser;
import soufix.common.Formulas;
import soufix.common.PathFinding;
import soufix.common.SocketManager;
import soufix.database.Database;
import soufix.entity.Collector;
import soufix.entity.Npc;
import soufix.entity.Prism;
import soufix.entity.monster.MobGrade;
import soufix.entity.monster.MobGroup;
import soufix.entity.monster.Monster;
import soufix.entity.mount.Mount;
import soufix.entity.npc.NpcMovable;
import soufix.entity.npc.NpcTemplate;
import soufix.fight.Fight;
import soufix.fight.Fighter;
import soufix.game.World;
import soufix.game.scheduler.entity.RespawnGroup;
import soufix.main.Config;
import soufix.main.Constant;
import soufix.main.Main;
import soufix.object.GameObject;
import soufix.object.entity.Capture;
import soufix.other.Action;
import soufix.utility.Pair;
import soufix.utility.TimerWaiterPlus;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;



public class GameMap
{
  public int nextObjectId=-1;
  public boolean noMarchand=false, noCollector=false, noPrism=false, noTP=false, noDefie=false, noAgro=false, noCanal=false;
  public short id;
  private String date, key, placesStr;
  private byte w, h, X=0, Y=0, maxGroup=3, maxSize, minSize, fixSize;
  private int maxTeam=0;
  private boolean isMute=false;
  private SubArea subArea;
  private MountPark mountPark;
  private List<GameCase> cases=new ArrayList<>();
  private List<Fight> fights=new ArrayList<>();
  private ArrayList<MobGrade> mobPossibles=new ArrayList<>();
  private Map<Integer, MobGroup> mobGroups=new HashMap<>();
  private Map<Integer, MobGroup> fixMobGroups=new HashMap<>();
  private Map<Integer, MobGroup> randomFixMobGroups=new HashMap<>();
  private Map<Integer, Npc> npcs=new HashMap<>();
  private Map<Integer, ArrayList<Action>> endFightAction=new HashMap<>();
  private Map<Integer, Integer> mobExtras=new HashMap<>();
  private int minRespawnTime;
  private int maxRespawnTime;
  private int Song;
  private String pos;
  private String Ddata;
  private long time_pose_perco;
	private Map<Short, Fight> _peleas;



public GameMap(short id, String date, byte w, byte h, String key, String places, String dData, String monsters, String mapPos, byte maxGroup, byte fixSize, byte minSize, byte maxSize, String forbidden, byte sniffed, int minRespawnTime, int maxRespawnTime, int song)
  {
    this.id=id;
    this.date=date;
    this.w=w;
    this.h=h;
    this.key=key;
    this.placesStr=places;
    this.maxGroup=maxGroup;
    this.maxSize=maxSize;
    this.minSize=minSize;
    this.fixSize=fixSize;
    this.Ddata=dData;
    this.Song=song;
    this.cases=Main.world.getCryptManager().decompileMapData(this,dData,sniffed);
    this.setMinRespawnTime(minRespawnTime);
    this.setMaxRespawnTime(maxRespawnTime);

    try
    {
      if(!places.equalsIgnoreCase("")&&!places.equalsIgnoreCase("|"))
        this.maxTeam=(places.split("\\|")[1].length()/2);
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    try
    {
      String[] mapInfos=mapPos.split(",");
      this.X=Byte.parseByte(mapInfos[0]);
      this.Y=Byte.parseByte(mapInfos[1]);
      int subArea=Integer.parseInt(mapInfos[2]);
      this.pos = mapInfos[0]+","+mapInfos[1];

      this.subArea=Main.world.getSubArea(subArea);
      if(this.subArea!=null)
        this.subArea.addMap(this);
    }
    catch(Exception e)
    {
      e.printStackTrace();
      Main.stop("GameMap1");
    }

    try
    {
      String[] split=forbidden.split(";");
      noMarchand=split[0].equals("1");
      noCollector=split[1].equals("1");
      noPrism=split[2].equals("1");
      noTP=split[3].equals("1");
      noDefie=split[4].equals("1");
      noAgro=split[5].equals("1");
      noCanal=split[6].equals("1");
    }
    catch(Exception e)
    {
    }
    /*
    int check = 0;
    for(String data : monsters.split("\\|"))
    {
    	if(data.equals(""))
            continue;
    	 String[] infos=data.split(",");
    	 if(infos[0].compareTo("490") == 0)
    		 check++;
    }
    if(check > 1) {
    	int change = check/2;
    	String  new_text = "";
    	int ok = 0;
    	 for(String data : monsters.split("\\|"))
    	    {
    		 String haha = data;
    	    	if(data.equals(""))
    	            continue;
    	    	String[] infos=data.split(",");
    	    	 if(infos[0].compareTo("490") == 0)
    	    	 if(ok < change) {
    	    		 System.out.println(haha);
    	    		 haha = data.replace("490","489");
    	    		 System.out.println(haha);
    	    		 ok++;
    	    	 }
    	    	 new_text += "|"+haha;
    	    }
    	 System.out.println(check);
    	 System.out.println(change);
    	 System.out.println(ok);
    	    System.out.println(new_text);
    	    Database.getDynamics().getMapData().updatemapmpster(this.id, new_text);
    }*/
    for(String mob : monsters.split("\\|"))
    {
      if(mob.equals(""))
        continue;
      int id1,lvl;
      try
      {
        id1=Integer.parseInt(mob.split(",")[0]);
        lvl=Integer.parseInt(mob.split(",")[1]);
      }
      catch(NumberFormatException e)
      {
        e.printStackTrace();
        continue;
      }
      if(id1==0||lvl==0)
        continue;
      if(Main.world.getMonstre(id1)==null)
        continue;
      if(Main.world.getMonstre(id1).getGradeByLevel(lvl)==null)
        continue;
      if(Config.getInstance().HALLOWEEN)
      {
        switch(id1)
        {
          case 98://Tofu
            if(Main.world.getMonstre(794)!=null)
              if(Main.world.getMonstre(794).getGradeByLevel(lvl)!=null)
                id1=794;
            break;
          case 101://Bouftou
            if(Main.world.getMonstre(793)!=null)
              if(Main.world.getMonstre(793).getGradeByLevel(lvl)!=null)
                id1=793;
            break;
        }
      }

      this.mobPossibles.add(Main.world.getMonstre(id1).getGradeByLevel(lvl));
    }
  }

  public GameMap(short id, String date, byte w, byte h, String key, String places, int minRespawnTime, int maxRespawnTime)
  {
    this.id=id;
    this.date=date;
    this.w=w;
    this.h=h;
    this.key=key;
    this.placesStr=places;
    this.cases=new ArrayList<>();
    this.setMinRespawnTime(minRespawnTime);
    this.setMaxRespawnTime(maxRespawnTime);
  }

  public GameMap(short id, String date, byte w, byte h, String key, String places, byte x, byte y, byte maxGroup, byte fixSize, byte minSize, byte maxSize, int minRespawnTime, int maxRespawnTime)
  {
    this.id=id;
    this.date=date;
    this.w=w;
    this.h=h;
    this.key=key;
    this.placesStr=places;
    this.X=x;
    this.Y=y;
    this.maxGroup=maxGroup;
    this.maxSize=maxSize;
    this.minSize=minSize;
    this.fixSize=fixSize;
    this.setMinRespawnTime(minRespawnTime);
    this.setMaxRespawnTime(maxRespawnTime);
  }

  public static void removeMountPark(int guildId)
  {
    try
    {
      Main.world.getMountPark().values().stream().filter(park -> park.getGuild()!=null).filter(park -> park.getGuild().getId()==guildId).forEach(park -> {
        if(!park.getListOfRaising().isEmpty())
        {
          for(Integer id : new ArrayList<>(park.getListOfRaising()))
          {
            if(Main.world.getMountById(id)==null)
            {
              park.delRaising(id);
              continue;
            }
            Main.world.removeMount(id);
            Database.getDynamics().getMountData().delete(id);
          }
          park.getListOfRaising().clear();
        }
        if(!park.getEtable().isEmpty())
        {
          for(Mount mount : new ArrayList<>(park.getEtable()))
          {
            if(mount==null)
              continue;
            Main.world.removeMount(mount.getId());
            Database.getDynamics().getMountData().delete(mount.getId());
          }
          park.getEtable().clear();
        }

        park.setOwner(0);
        park.setGuild(null);
        park.setPrice(3000000);
        Database.getDynamics().getMountParkData().update(park);

        for(Player p : park.getMap().getPlayers())
          SocketManager.GAME_SEND_Rp_PACKET(p,park);
      });
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  public static int getObjResist(Player perso, int cellid, int itemID)
  {
    MountPark MP=perso.getCurMap().getMountPark();
    String packets="";
    if(MP==null||MP.getObject().size()==0)
      return 0;
    for(Entry<Integer, Map<Integer, Integer>> entry : MP.getObjDurab().entrySet())
    {
      for(Entry<Integer, Integer> entry2 : entry.getValue().entrySet())
      {
        if(cellid==entry.getKey())
          packets+=entry.getKey()+";"+entry2.getValue()+";"+entry2.getKey();
      }
    }
    int cell,durability,durabilityMax;
    try
    {
      String[] infos=packets.split(";");
      cell=Integer.parseInt(infos[0]);
      if(itemID==7798||itemID==7605||itemID==7606||itemID==7625||itemID==7628||itemID==7634)
      {
        durability=Integer.parseInt(infos[1]);
      }
      else
      {
        durability=Integer.parseInt(infos[1])-1;
      }
      durabilityMax=Integer.parseInt(infos[2]);
    }
    catch(Exception e)
    {
      e.printStackTrace();
      return 0;
    }

    if(durability<=0)
    {
      //if (MP.delObject(cell)) {
      durability=0;
      Map<Integer, Integer> InDurab=new HashMap<>();
      InDurab.put(durabilityMax,durability);
      MP.getObjDurab().put(cell,InDurab);
      SocketManager.SEND_GDO_PUT_OBJECT_MOUNT(perso.getCurMap(),cell+";"+itemID+";1;"+durability+";"+durabilityMax);
      return 0;
      //}
    }
    else
    {
      Map<Integer, Integer> InDurab=new HashMap<>();
      InDurab.put(durabilityMax,durability);
      MP.getObjDurab().put(cell,InDurab);
      SocketManager.SEND_GDO_PUT_OBJECT_MOUNT(perso.getCurMap(),cell+";"+itemID+";1;"+durability+";"+durabilityMax);
    }
    return durabilityMax;
  }

  public static int getObjResist(MountPark MP, int cellid, int itemID)
  {
    String packets="";
    if(MP==null||MP.getObject().size()==0)
      return 0;
    for(Entry<Integer, Map<Integer, Integer>> entry : MP.getObjDurab().entrySet())
    {
      for(Entry<Integer, Integer> entry2 : entry.getValue().entrySet())
      {
        if(cellid==entry.getKey())
          packets+=entry.getKey()+";"+entry2.getValue()+";"+entry2.getKey();
      }
    }
    String[] infos=packets.split(";");
    int cell=Integer.parseInt(infos[0]),durability;
    if(itemID==7798||itemID==7605||itemID==7606||itemID==7625||itemID==7628||itemID==7634)
    {
      durability=Integer.parseInt(infos[1]);
    }
    else
    {
      durability=Integer.parseInt(infos[1])-1;
    }
    int durabilityMax=Integer.parseInt(infos[2]);

    if(durability<=0)
    {
      //if (MP.delObject(cell)) {
      durability=0;
      Map<Integer, Integer> InDurab=new HashMap<>();
      InDurab.put(durabilityMax,durability);
      MP.getObjDurab().put(cell,InDurab);
      SocketManager.SEND_GDO_PUT_OBJECT_MOUNT(MP.getMap(),cell+";"+itemID+";1;"+durability+";"+durabilityMax);
      return 0;
      //}
    }
    else
    {
      Map<Integer, Integer> InDurab=new HashMap<>();
      InDurab.put(durabilityMax,durability);
      MP.getObjDurab().put(cell,InDurab);
      SocketManager.SEND_GDO_PUT_OBJECT_MOUNT(MP.getMap(),cell+";"+itemID+";1;"+durability+";"+durabilityMax);
    }
    return durabilityMax;
  }

  public void setInfos(String date, String monsters, String mapPos, byte maxGroup, byte fixSize, byte minSize, byte maxSize, String forbidden)
  {
    this.date=date;
    this.mobPossibles.clear();

    try
    {
      String[] split=forbidden.split(";");
      noMarchand=split[0].equals("1");
      noCollector=split[1].equals("1");
      noPrism=split[2].equals("1");
      noTP=split[3].equals("1");
      noDefie=split[4].equals("1");
      noAgro=split[5].equals("1");
      noCanal=split[6].equals("1");
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }

    for(String mob : monsters.split("\\|"))
    {
      if(mob.equals(""))
        continue;
      int id1,lvl;
      try
      {
        id1=Integer.parseInt(mob.split(",")[0]);
        lvl=Integer.parseInt(mob.split(",")[1]);
      }
      catch(NumberFormatException e)
      {
        e.printStackTrace();
        continue;
      }
      if(id1==0||lvl==0)
        continue;
      if(Main.world.getMonstre(id1)==null)
        continue;
      if(Main.world.getMonstre(id1).getGradeByLevel(lvl)==null)
        continue;
      this.mobPossibles.add(Main.world.getMonstre(id1).getGradeByLevel(lvl));
    }
    try
    {
      String[] mapInfos=mapPos.split(",");
      this.X=Byte.parseByte(mapInfos[0]);
      this.Y=Byte.parseByte(mapInfos[1]);
      int subArea=Integer.parseInt(mapInfos[2]);
      if(subArea==0&&id==32)
      {
        this.subArea=Main.world.getSubArea(subArea);
        if(this.subArea!=null)
          this.subArea.addMap(this);
      }
      else if(subArea!=0)
      {
        this.subArea=Main.world.getSubArea(subArea);
        if(this.subArea!=null)
          this.subArea.addMap(this);
      }
    }
    catch(Exception e)
    {
      e.printStackTrace();
      Main.stop("GameMap2");
    }
    this.maxGroup=maxGroup;
    this.maxSize=maxSize;
    this.minSize=minSize;
    this.fixSize=fixSize;
  }

  public void addMobExtra(Integer id, Integer chances)
  {
    this.mobExtras.put(id,chances);
  }
  public int getSong() {
	return Song;
}

  public void setGs(byte maxGroup, byte minSize, byte fixSize, byte maxSize)
  {
    this.maxGroup=maxGroup;
    this.maxSize=maxSize;
    this.minSize=minSize;
    this.fixSize=fixSize;
  }

  public ArrayList<MobGrade> getMobPossibles()
  {
    return this.mobPossibles;
  }

  public void setMobPossibles(String monsters)
  {
    if(monsters==null||monsters.equals(""))
      return;

    this.mobPossibles=new ArrayList<>();

    for(String mob : monsters.split("\\|"))
    {
      if(mob.equals(""))
        continue;
      int id1,lvl;
      try
      {
        id1=Integer.parseInt(mob.split(",")[0]);
        lvl=Integer.parseInt(mob.split(",")[1]);
      }
      catch(NumberFormatException e)
      {
        e.printStackTrace();
        continue;
      }
      if(id1==0||lvl==0)
        continue;
      if(Main.world.getMonstre(id1)==null)
        continue;
      if(Main.world.getMonstre(id1).getGradeByLevel(lvl)==null)
        continue;
      if(Config.getInstance().HALLOWEEN)
      {
        switch(id1)
        {
          case 98://Tofu
            if(Main.world.getMonstre(794)!=null)
              if(Main.world.getMonstre(794).getGradeByLevel(lvl)!=null)
                id1=794;
            break;
          case 101://Bouftou
            if(Main.world.getMonstre(793)!=null)
              if(Main.world.getMonstre(793).getGradeByLevel(lvl)!=null)
                id1=793;
            break;
        }
      }

      this.mobPossibles.add(Main.world.getMonstre(id1).getGradeByLevel(lvl));
    }
  }

  public byte getMaxSize()
  {
    return this.maxSize;
  }

  public byte getMinSize()
  {
    return this.minSize;
  }

  public byte getFixSize()
  {
    return this.fixSize;
  }

  public short getId()
  {
    return id;
  }

  public String getDate()
  {
    return date;
  }

  public byte getW()
  {
    return w;
  }

  public byte getH()
  {
    return h;
  }

  public String getKey()
  {
    return key;
  }

  public String getPlaces()
  {
    return placesStr;
  }

  public void setPlaces(String place)
  {
    this.placesStr=place;
  }


  public List<GameCase> getCases()
  {
    return cases;
  }
  private void setCases(List<GameCase> cases)
  {
    this.cases=cases;
  }

  public GameCase getCase(int id)
  {
    for(GameCase gameCase : this.cases)
      if(gameCase.getId()==(id))
        return gameCase;
    return null;
  }


  public void removeCase(int id)
  {
    Iterator<GameCase> iterator=this.cases.iterator();

    while(iterator.hasNext())
    {
    	try
        {
      GameCase gameCase=iterator.next();
      if(gameCase!=null&&gameCase.getId()==id)
      {
        iterator.remove();
        break;
      }
    }
    catch(NumberFormatException e)
    {
      e.printStackTrace();
      break;
    }
    	
    }
  }

  public Fight newFight(Player init1, Player init2, int type)
  {
    if(init1.getFight()!=null||init2.getFight()!=null)
      return null;
    int id=1;
    if(this.fights==null)
      this.fights=new ArrayList<>();
    if(!this.fights.isEmpty())
      id=((Fight)(this.fights.toArray()[this.fights.size()-1])).getId()+1;
    Fight f=new Fight(type,id,this,init1,init2);
    this.fights.add(f);
    SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(this);
    return f;
  }

  public void removeFight(int id)
  {
    if(this.fights!=null)
    {
      Iterator<Fight> iterator=this.getFights().iterator();
      while(iterator.hasNext())
      {
        Fight fight=iterator.next();
        if(fight!=null&&fight.getId()==id)
        {
          iterator.remove();
          break;
        }
      }

      if(this.fights.isEmpty())
        this.fights=null;
    }
  }

  public int getNbrFight()
  {
    return fights==null ? 0 : this.fights.size();
  }

  public Fight getFight(int id)
  {
    if(this.fights!=null)
      for(Fight fight : this.fights)
        if(fight.getId()==id)
          return fight;
    return null;
  }

  public List<Fight> getFights()
  {
    if(this.fights==null)
      return new ArrayList<>();
    return fights;
  }
  public Map<Short, Fight> getPeleas() {
		return _peleas;
	}

  public Map<Integer, MobGroup> getMobGroups()
  {
    return this.mobGroups;
  }

  public void removeNpcOrMobGroup(int id)
  {
    this.npcs.remove(id);
    this.mobGroups.remove(id);
  }

  public Npc addNpc(int npcID, int cellID, int dir)
  {
    NpcTemplate temp=Main.world.getNPCTemplate(npcID);
    if(temp==null)
      return null;
    if(getCase(cellID)==null)
      return null;
    Npc npc;
    if(temp.getPath().isEmpty())
      npc=new Npc(-7000-this.nextObjectId,cellID,(byte)dir,temp);
    else
      npc=new NpcMovable(-7000-this.nextObjectId,cellID,(byte)dir,this.id,temp);
    this.npcs.put(-7000-this.nextObjectId,npc);
    this.nextObjectId--;
    return npc;
  }

  public Map<Integer, Npc> getNpcs()
  {
    return this.npcs;
  }

  public Npc getNpc(int id)
  {
    return this.npcs.get(id);
  }

  public Npc RemoveNpc(int id)
  {
    return this.npcs.remove(id);
  }

  public void applyEndFightAction(Player player)
  {
    if(this.endFightAction.get(player.needEndFight())==null)
      return;
    if(this.id==8545)
    {
      if(player.getCurCell().getId()<=193&&player.getCurCell().getId()!=186&&player.getCurCell().getId()!=187&&player.getCurCell().getId()!=173&&player.getCurCell().getId()!=172&&player.getCurCell().getId()!=144&&player.getCurCell().getId()!=158)
      {
        for(Action A : this.endFightAction.get(player.needEndFight()))
        {
          A.apply(player,null,-1,-1);
        }
      }
      else
      {
        for(Action A : this.endFightAction.get(player.needEndFight()))
        {
          A.setArgs("8547,214");
          A.apply(player,null,-1,-1);
        }
      }
    }
    else
    {
      for(Action A : this.endFightAction.get(player.needEndFight()))
        A.apply(player,null,-1,-1);
    }
    player.setNeededEndFight(-1,null);
  }

  public boolean hasEndFightAction(int type)
  {
    return this.endFightAction.get(type)!=null;
  }

  public void addEndFightAction(int type, Action A)
  {
    if(this.endFightAction.get(type)==null)
      this.endFightAction.put(type,new ArrayList<>()); // On retire l'action si elle existait dï¿½jï¿½
    delEndFightAction(type,A.getId());
    this.endFightAction.get(type).add(A);
  }

  public void delEndFightAction(int type, int aType)
  {
    if(this.endFightAction.get(type)!=null)
      new ArrayList<>(this.endFightAction.get(type)).stream().filter(A -> A.getId()==aType).forEach(A -> this.endFightAction.get(type).remove(A));
  }

  public void delAllEndFightAction()
  {
    this.endFightAction.clear();
  }

  public int getX()
  {
    return this.X;
  }

  public int getY()
  {
    return this.Y;
  }

  public SubArea getSubArea()
  {
    return this.subArea;
  }

  public MountPark getMountPark()
  {
    return this.mountPark;
  }

  public void setMountPark(MountPark mountPark)
  {
    this.mountPark=mountPark;
  }

  public int getMaxGroupNumb()
  {
    return this.maxGroup;
  }

  public int getMaxTeam()
  {
    return this.maxTeam;
  }
  public String getDdata() {
	return Ddata;
}

  public boolean containsForbiddenCellSpawn(int id)
  {
    if(this.mountPark!=null)
      if(this.mountPark.getCellAndObject().containsKey(id))
        return true;
    return false;
  }

  public GameMap getMapCopy()
  {
    List<GameCase> cases=new ArrayList<>();

    GameMap map=new GameMap(id,date,w,h,key,placesStr,minRespawnTime,maxRespawnTime);

    for(GameCase gameCase : this.cases)
    {
      if(map.getId()==8279)
      {
        switch(gameCase.getId())
        {
          case 187:
          case 170:
          case 156:
          case 142:
          case 128:
          case 114:
          case 100:
          case 86:
            continue;
        }
      }

      cases.add(new GameCase(map,gameCase.getId(),gameCase.getActivo(),gameCase.getMovimiento(),gameCase.getLevel(),gameCase.getSlope(),gameCase.isWalkable(true,true,-1),gameCase.isLoS(),(gameCase.getObject()==null ? -1 : gameCase.getObject().getId())));
    }
    map.setCases(cases);

    return map;
  }

  public GameMap getMapCopyIdentic()
  {
    GameMap map=new GameMap(id,date,w,h,key,placesStr,X,Y,maxGroup,fixSize,minSize,maxSize,minRespawnTime,maxRespawnTime);
    List<GameCase> cases=this.cases.stream().map(entry -> new GameCase(map,entry.getId(),entry.getActivo(),entry.getMovimiento(),entry.getLevel(),entry.getSlope(),entry.isWalkable(false),entry.isLoS(),(entry.getObject()==null ? -1 : entry.getObject().getId()))).collect(Collectors.toList());
    map.setCases(cases);
    return map;
  }

  //v2.8 - undefined cell login fix
  public void addPlayer(Player perso)
  {
    SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(this,perso);
    if(perso.getCurCell()!=null)
      perso.getCurCell().addPlayer(perso);
    else
      this.getCase(this.getRandomFreeCellId()).addPlayer(perso);
    if(perso.getEnergy()>0)
    {
      if(perso.getEnergy()>=10000)
        return;
      if(Constant.isTaverne(this)&&perso.getTimeTaverne()==0)
      {
        perso.setTimeTaverne(System.currentTimeMillis());
      }
      else if(perso.getTimeTaverne()!=0)
      {
        int gain=(int)((System.currentTimeMillis()-perso.getTimeTaverne())/1000);
        if(gain>=10000)
          gain=10000-perso.getEnergy();
        perso.setEnergy(perso.getEnergy()+gain);
        if(perso.getEnergy()>=10000)
          perso.setEnergy(10000);
        SocketManager.GAME_SEND_Im_PACKET(perso,"092;"+gain);
        SocketManager.GAME_SEND_STATS_PACKET(perso);
        perso.setTimeTaverne(0);
      }
    }
  }

  public  ArrayList<Player> getPlayers()
  {
    ArrayList<Player> player=new ArrayList<>();
    for(GameCase c : cases)
      if(!c.getPlayers().isEmpty())
        for(Player p : c.getPlayers())
          if(!player.contains(p))
            player.add(p);
    return player;
  }

  public void sendFloorItems(Player perso)
  {
    this.cases.stream().filter(c -> c.getDroppedItem(false)!=null).forEach(c -> SocketManager.GAME_SEND_GDO_PACKET(perso,'+',c.getId(),c.getDroppedItem(false).getTemplate().getId(),0));
  }

  public void delAllDropItem()
  {
    for(GameCase gameCase : this.cases)
    {
      SocketManager.GAME_SEND_GDO_PACKET_TO_MAP(this,'-',gameCase.getId(),0,0);
      gameCase.clearDroppedItem();
    }
  }

  public int getStoreCount()
  {
    if(Main.world.getSeller(this.getId())==null)
      return 0;
    return Main.world.getSeller(this.getId()).size();
  }

  public boolean haveMobFix()
  {
    if(this.randomFixMobGroups.size()>0)
      return this.randomFixMobGroups.size()>0;
    else
      return this.fixMobGroups.size()>0;
  }

  public boolean isPossibleToPutMonster()
  {
    return !this.cases.isEmpty()&&this.maxGroup>0&&this.mobPossibles.size()>0;
  }

  public boolean loadExtraMonsterOnMap(int idMob)
  {
    if(Main.world.getMonstre(idMob)==null)
      return false;
    MobGrade grade=Main.world.getMonstre(idMob).getRandomGrade();
    int cell=this.getRandomFreeCellId();
    MobGroup group=new MobGroup(this.nextObjectId,Constant.ALIGNEMENT_NEUTRE,this.mobPossibles,this,cell,this.fixSize,this.maxSize,this.maxSize,grade,false);
    if(group.getMobs().isEmpty())
      return false;
    this.mobGroups.put(this.nextObjectId,group);
    this.nextObjectId--;
    return true;
  }

  public void loadMonsterOnMap()
  {
    if(this.maxGroup==0)
      return;
    spawnGroup(Constant.ALIGNEMENT_NEUTRE,this.maxGroup,false,-1);//Spawn des groupes d'alignement neutre
    spawnGroup(Constant.ALIGNEMENT_BONTARIEN,1,false,-1);//Spawn du groupe de gardes bontarien s'il y a
    spawnGroup(Constant.ALIGNEMENT_BRAKMARIEN,1,false,-1);//Spawn du groupe de gardes brakmarien s'il y a
  }

  public void mute()
  {
    this.isMute=!this.isMute;
  }

  public boolean isMute()
  {
    return this.isMute;
  }

  public boolean isAggroByMob(Player perso, int cell)
  {
    if(placesStr.equalsIgnoreCase("|"))
      return false;
    if(perso.getCurMap().getId()!=id||!perso.canAggro())
      return false;
    for(MobGroup group : this.mobGroups.values())
    {
    	if(perso.get_align() == 0 || group.getAlignement()>=0)
    		continue;
      if(perso.get_align()==0&&group.getAlignement()>=0) //neutral player and not neutral mob
        continue;
      if(perso.get_align()==1&&group.getAlignement()==1) //same align
        continue;
      if(perso.get_align()==2&&group.getAlignement()==2) //same align
        continue;

      if(this.subArea!=null)
      {
        group.setSubArea(this.subArea.getId());
        group.changeAgro();
      }
      if(PathFinding.getDistanceBetween(this,cell,group.getCellId())<=group.getAggroDistance()&&group.getAggroDistance()>0)//S'il y aggro
      {
        if(ConditionParser.validConditions(perso,group.getCondition()))
          return true;
      }
    }
    return false;
  }

  public void spawnAfterTimeGroup(int minRespawn, int maxRespawn)
  {
	  TimerWaiterPlus.addNext(new RespawnGroup(this,-1,System.currentTimeMillis()),Formulas.getRandomValue(minRespawn,maxRespawn), TimerWaiterPlus.DataType.MAP);
  }

  public void spawnAfterTimeGroupFix(final int cell, int minRespawn, int maxRespawn)
  {
		  
	  TimerWaiterPlus.addNext(new RespawnGroup(this,cell,System.currentTimeMillis()),Formulas.getRandomValue(minRespawn,maxRespawn), TimerWaiterPlus.DataType.MAP);
  }

  public void spawnGroup(int align, int nbr, boolean log, int cellID)
  {
    if(nbr<1)
      return;
    if(this.mobGroups.size()+this.fixMobGroups.size()+this.randomFixMobGroups.size()>=this.maxGroup)
      return;
    ArrayList<Short> s1 = Main.world.getMapsStars(id);
   
    for(int a=1;a<=nbr;a++)
    {
    	 short stars = -1;
    	    if (s1 != null && !s1.isEmpty()) {
    	        if(s1.get(a-1) != null)
    	            stars = s1.get(a-1);
    	    }
      // mobExtras
      ArrayList<MobGrade> mobPoss=new ArrayList<>(this.mobPossibles);
      if(!this.mobExtras.isEmpty())
      {
        for(Entry<Integer, Integer> entry : this.mobExtras.entrySet())
        {
          if(entry.getKey()==499) // Si c'est un minotoboule de nowel
            if(!Config.getInstance().NOEL) // Si ce n'est pas nowel
              continue;
          int random=Formulas.getRandomValue(0,99);
          while(entry.getValue()>random)
          {
            Monster mob=Main.world.getMonstre(entry.getKey());
            if(mob==null)
              continue;
            MobGrade mobG=mob.getRandomGrade();
            if(mobG==null)
              continue;
            mobPoss.add(mobG);
            if(entry.getKey()==422||entry.getKey()==499) // un seul DDV / Minotoboule
              break;
            random=Formulas.getRandomValue(0,99);
          }
        }
      }

      while(this.mobGroups.get(this.nextObjectId)!=null)
        this.nextObjectId--;

      MobGroup group=new MobGroup(this.nextObjectId,align,mobPoss,this,cellID,this.fixSize,this.minSize,this.maxSize,null,true);

      if(group.getMobs().isEmpty())
        continue;
      group.setSpawnTime(System.currentTimeMillis());
      group.SetBonusStars(stars);
      this.mobGroups.put(this.nextObjectId,group);
      if(log)
        SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this,group);
      this.nextObjectId--;
    }
  }
  
  
  public void RespawnGroup(int align, int nbr, boolean log, int cellID)
  {
    if(nbr<1)
      return;
    if(this.mobGroups.size()+this.fixMobGroups.size()+this.randomFixMobGroups.size()>=this.maxGroup)
      return;
   
    
    for(int a=1;a<=nbr;a++)
    {
    	  
    		
      // mobExtras
      ArrayList<MobGrade> mobPoss=new ArrayList<>(this.mobPossibles);
      if(!this.mobExtras.isEmpty())
      {
        for(Entry<Integer, Integer> entry : this.mobExtras.entrySet())
        {
          if(entry.getKey()==499) // Si c'est un minotoboule de nowel
            if(!Config.getInstance().NOEL) // Si ce n'est pas nowel
              continue;
          int random=Formulas.getRandomValue(0,99);
          while(entry.getValue()>random)
          {
            Monster mob=Main.world.getMonstre(entry.getKey());
            if(mob==null)
              continue;
            MobGrade mobG=mob.getRandomGrade();
            if(mobG==null)
              continue;
            mobPoss.add(mobG);
            if(entry.getKey()==422||entry.getKey()==499) // un seul DDV / Minotoboule
              break;
            random=Formulas.getRandomValue(0,99);
          }
        }
      }

      while(this.mobGroups.get(this.nextObjectId)!=null)
        this.nextObjectId--;

      MobGroup group=new MobGroup(this.nextObjectId,align,mobPoss,this,cellID,this.fixSize,this.minSize,this.maxSize,null,true);

      if(group.getMobs().isEmpty())
        continue;
      group.SetBonusStars(0);

      this.mobGroups.put(this.nextObjectId,group);
      group.setSpawnTime(System.currentTimeMillis());
      if(log)
        SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this,group);
      this.nextObjectId--;
    }
  }

  public void respawnGroup(MobGroup group)
  {
    this.mobGroups.put(group.getId(),group);
    group.SetBonusStars(0);
    SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this,group);
  }

  public void spawnGroupWith(Monster m)
  {
    while(this.mobGroups.get(this.nextObjectId)!=null)
      this.nextObjectId--;
    MobGrade _m=null;
    while(_m==null)
      _m=m.getRandomGrade();
    int cell=this.getRandomFreeCellId();
    while(this.containsForbiddenCellSpawn(cell))
      cell=this.getRandomFreeCellId();

    MobGroup group=new MobGroup(this.nextObjectId,-1,this.mobPossibles,this,cell,this.fixSize,this.minSize,this.maxSize,_m,false);
    group.setIsFix(false);
    this.mobGroups.put(this.nextObjectId,group);
    SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this,group);
    this.nextObjectId--;
  }

  public void spawnNewGroup(boolean timer, int cellID, String groupData, String condition, int guid,String ip)
  {
	  if(this.id == 7411)
		  return;
    while(this.mobGroups.get(this.nextObjectId)!=null)
      this.nextObjectId--;
    while(this.containsForbiddenCellSpawn(cellID))
      cellID=this.getRandomFreeCellId();

    MobGroup group=new MobGroup(this.nextObjectId,cellID,groupData);
    if(group.getMobs().isEmpty())
      return;
    this.mobGroups.put(this.nextObjectId,group);
    group.setCondition(condition);
    group.set_ID_PLAYER(guid);
    group.setIsFix(false);
    group.setIp(ip);
    SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this,group);
    this.nextObjectId--;
    if(timer)
      group.startCondTimer();
  }
  public void spawnNewGroupCapture(boolean timer, int cellID, String groupData, int condition) {
      while(this.mobGroups.get(this.nextObjectId) != null)
          this.nextObjectId--;
      while (this.containsForbiddenCellSpawn(cellID))
          cellID = this.getRandomFreeCellId();

     MobGroup group = new MobGroup(this.nextObjectId, cellID, groupData);
      if (group.getMobs().isEmpty())
          return;
      this.mobGroups.put(this.nextObjectId, group);
      group.capturer = condition;
      group.setIsFix(false);
      SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this, group);
      this.nextObjectId--;
      if (timer)
          group.startCondTimer();
  }

  public void spawnGroupOnCommand(int cellID, String groupData, boolean send)
  {
    while(this.mobGroups.get(this.nextObjectId)!=null)
      this.nextObjectId--;
    MobGroup group=new MobGroup(this.nextObjectId,cellID,groupData);
    if(group.getMobs().isEmpty())
      return;
    this.mobGroups.put(this.nextObjectId,group);
    group.setIsFix(false);
    if(send)
      SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this,group);

    this.nextObjectId--;
  }
  public void spawnGroupOnCommandfix(int cellID, String groupData, boolean send)
  {
    while(this.mobGroups.get(this.nextObjectId)!=null)
      this.nextObjectId--;
    MobGroup group=new MobGroup(this.nextObjectId,cellID,groupData);
    if(group.getMobs().isEmpty())
      return;
    this.mobGroups.put(this.nextObjectId,group);
    group.setIsFix(true);
    if(send)
      SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this,group);

    this.nextObjectId--;
  }

  //v2.8 - fixed fixmobgroup restart, fixed bug where they didnt get set to 0 stars
  public void addStaticGroupv2(int cellID, String groupData, boolean b, Long time, int stars)
  {
    while(this.mobGroups.get(this.nextObjectId)!=null)
      this.nextObjectId--;

    MobGroup group=new MobGroup(this.nextObjectId,cellID,groupData);
    if(group.getMobs().isEmpty())
      return;
    group.setSpawnTime(time);
    group.SetBonusStars(stars);
    this.mobGroups.put(this.nextObjectId,group);
    this.nextObjectId--;
    this.fixMobGroups.put(-1000+this.nextObjectId,group);
    if(b)
      SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this,group);
  }
 /* public void addStaticGroup(int cellID, String groupData, boolean b)
  {
    while(this.mobGroups.get(this.nextObjectId)!=null)
      this.nextObjectId--;
    MobGroup group=new MobGroup(this.nextObjectId,cellID,groupData);
    if(group.getMobs().isEmpty())
      return;
    //group.setInternalStarBonus(stars);
    this.mobGroups.put(this.nextObjectId,group);
    this.nextObjectId--;
    this.fixMobGroups.put(-1000+this.nextObjectId,group);
    if(b)
      SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this,group);
  }
*/
  //v2.8 - fixed fixmobgroup restart, fixed bug where they didnt get set to 0 stars
  public void addStaticGroup(int cellID, String groupData, boolean b, int stars)
  {
    while(this.mobGroups.get(this.nextObjectId)!=null)
      this.nextObjectId--;
    MobGroup group=new MobGroup(this.nextObjectId,cellID,groupData);
    if(group.getMobs().isEmpty())
      return;
    group.SetBonusStars(stars);
    group.setSpawnTime(System.currentTimeMillis());
    this.mobGroups.put(this.nextObjectId,group);
    this.nextObjectId--;
    this.fixMobGroups.put(-1000+this.nextObjectId,group);
    if(b)
      SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this,group);
  }

  //v2.3 - random dungeon arrayoutofbounds fix
  public void addRandomStaticGroup(int cellID, String randomGroupData, boolean b, int stars)
  {
    while(this.mobGroups.get(this.nextObjectId)!=null)
      this.nextObjectId--;
    String randomGroup="";
    List<String> splitGroup=Arrays.asList(randomGroupData.split(":"));
    if(splitGroup.size()==3) //boss exists
    {
      if(splitGroup.get(2).compareTo("")!=0)
      {
        List<String> bosses=Arrays.asList(splitGroup.get(2).split(";"));
        randomGroup+=bosses.get(Formulas.getRandomValue(0,bosses.size()-1))+";";
        if(splitGroup.get(1).compareTo("")!=1) //monsters exist and boss exists
        {
          List<String> monsters=Arrays.asList(splitGroup.get(1).split(";"));
          for(int i=0;i<Integer.parseInt(splitGroup.get(0))-1;i++) //groupSize-1 monsters and one boss
            randomGroup+=monsters.get(Formulas.getRandomValue(0,monsters.size()-1))+";";
        }
      }
    }
    else if(splitGroup.size()==2) //monsters exist and boss does not exist
    {
      List<String> monsters=Arrays.asList(splitGroup.get(1).split(";"));
      for(int i=0;i<Integer.parseInt(splitGroup.get(0));i++) //groupSize monsters
        randomGroup+=monsters.get(Formulas.getRandomValue(0,monsters.size()-1))+";";
    }
    MobGroup group=new MobGroup(this.nextObjectId,cellID,randomGroup);
    if(group.getMobs().isEmpty())
      return;
    group.setSpawnTime(System.currentTimeMillis());
    group.SetBonusStars(stars);
    this.mobGroups.put(this.nextObjectId,group);
    this.nextObjectId--;
    this.randomFixMobGroups.put(-1000+this.nextObjectId,group);
       if(b)
      SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this,group);
  }

  public void refreshSpawns()
  {
    for(int id : this.mobGroups.keySet())
    {
      SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this,id);
    }
    this.mobGroups.clear();
    //this.mobGroups.putAll(this.fixMobGroups);
    for(MobGroup mg : this.fixMobGroups.values()) {
        //SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this,mg);
        this.spawnGroupOnCommand(mg.getCellId(),mg.Groupe_to_string(), true);
    }
    for(MobGroup mg : this.randomFixMobGroups.values())
      SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this,mg);

    spawnGroup(Constant.ALIGNEMENT_NEUTRE,this.maxGroup,true,-1);//Spawn des groupes d'alignement neutre
    spawnGroup(Constant.ALIGNEMENT_BONTARIEN,1,true,-1);//Spawn du groupe de gardes bontarien s'il y a
    spawnGroup(Constant.ALIGNEMENT_BRAKMARIEN,1,true,-1);//Spawn du groupe de gardes brakmarien s'il y a
  }

  public String getGMsPackets()
  {
	  try {	  
		  StringBuilder packet=new StringBuilder();
    cases.stream().filter(cell -> cell!=null).forEach(cell -> cell.getPlayers().stream().filter(player -> player!=null).forEach(player -> packet.append("GM|+").append(player.parseToGM()).append('\u0000')));
    return packet.toString();
	  }catch(Exception e)
	  {
		  StringBuilder packet=new StringBuilder();
		    cases.stream().filter(cell -> cell!=null).forEach(cell -> cell.getPlayers().stream().filter(player -> player!=null).forEach(player -> packet.append("GM|+").append(player.parseToGM()).append('\u0000')));
		    return packet.toString();
	  }
  }

  public String getFightersGMsPackets(Fight fight)
  {
	  try {	  
    StringBuilder packet=new StringBuilder("GM");
    for(GameCase cell : this.cases)
      cell.getFighters().stream().filter(fighter -> fighter.getFight()==fight).forEach(fighter -> packet.append("|").append(fighter.getGmPacket('+',false)));
    return packet.toString();
	  }catch(Exception e)
	  {
		  StringBuilder packet=new StringBuilder("GM");
		    for(GameCase cell : this.cases)
		      cell.getFighters().stream().filter(fighter -> fighter.getFight()==fight).forEach(fighter -> packet.append("|").append(fighter.getGmPacket('+',false)));
		    return packet.toString();  
	  }
  }

  public synchronized String getFighterGMPacket(Player player)
  {
    Fighter target=player.getFight().getFighterByPerso(player);
    for(GameCase cell : this.cases)
      for(Fighter fighter : cell.getFighters())
        if(fighter.getFight()==player.getFight()&&fighter==target)
          return "GM|"+fighter.getGmPacket('~',false);
    return "";
  }

  public synchronized String getMobGroupGMsPackets(Player perso)
  {
    if(this.mobGroups.isEmpty())
      return "";

    StringBuilder packet=new StringBuilder();
    packet.append("GM|");
    boolean isFirst=true;
    int max = 0;
    for(MobGroup entry : this.mobGroups.values())
    {
    	 if(!Capture.isInArenaMap(this.getId())){
    	max++;
    	if(max > 10)
    		continue;
    	 }
      String GM=entry.parseGM(perso);
      if(GM.equals(""))
        continue;

      if(!isFirst)
        packet.append("|");

      packet.append(GM);
      isFirst=false;
    }
    return packet.toString();
  }

  public synchronized String getPrismeGMPacket()
  {
    String str="";
    Collection<Prism> prisms=Main.world.AllPrisme();
    if(prisms!=null)
    {
      for(Prism prism : prisms)
      {
        if(prism.getMap()==this.id)
        {
          str=prism.getGMPrisme();
          break;
        }
      }
    }
    return str;
  }

  public synchronized String getNpcsGMsPackets(Player p)
  {
    if(this.npcs.isEmpty())
      return "";

    StringBuilder packet=new StringBuilder();
    packet.append("GM|");
    boolean isFirst=true;
    for(Entry<Integer, Npc> entry : this.npcs.entrySet())
    {
      String GM=entry.getValue().parse(false,p);
      if(GM.equals(""))
        continue;

      if(!isFirst)
        packet.append("|");

      packet.append(GM);
      isFirst=false;
    }
    return packet.toString();
  }

  public synchronized String getObjectsGDsPackets()
  {
    StringBuilder packet=new StringBuilder("GDF");
    try {
    this.cases.stream().filter(gameCase -> gameCase.getObject()!=null).forEach(
    		gameCase -> 
    		packet.append("|")
    		.append(gameCase.getId()).append(";").
    		append(gameCase.getObject().getState()).
    		append(";").append((gameCase.getObject().isInteractive() ? "1" : "0")));
    } catch (final Exception e) {
		e.printStackTrace();
	}
    return packet.toString();
  }

  //v2.7 - Tactical mode memory
  //v2.8 - Better follower system
  //v2.8 - Fixed follower teleporting
  //v2.8 - Fixed starting fights against newly spawned monsters
  //v2.8 - Save old map and cell
  @SuppressWarnings("unused")
  public void startFightVersusMonstres(Player perso, MobGroup group)
  {
    if(perso.getFight()!=null)
      return;
    if(this.placesStr.isEmpty()||this.placesStr.equals("|"))
    {
      perso.sendMessage("Cette carte n'a pas de positions de combat, veuillez poster le mapID (type / mapid) et ce message dans le canal de rapport de bug sur <b><a href='"+Config.getInstance().discord+"'>Discord</a></b>.");
      return;
    }
    
    if(Config.getInstance().fightAsBlocked||perso.isDead()==1||perso.afterFight||!perso.canAggro())
      return;
   /* if(perso.getGroupe() != null)
        if(perso.getGroupe().getId() >= 1 && perso.getGroupe().getId() < 6) {
        
        	SocketManager.GAME_SEND_MESSAGE(perso,"You can not join this fight as it is forbidden.");
            return;
        	}*/
    if(perso.getCurMap().getSubArea().getId()  == 115 || perso.getCurMap().getSubArea().getId()  == 117 ||
    		perso.getCurMap().getSubArea().getId()  == 118 ||perso.getCurMap().getSubArea().getId()  == 116){
    	return;
    }
    if(perso.get_align()==0&&group.getAlignement()>0)
      return;
    if(perso.get_align()==1&&group.getAlignement()==1)
      return;
    if(perso.get_align()==2&&group.getAlignement()==2)
      return;
    if (!group.getCondition().equals("") || group.capturer != 0)
        if (!ConditionParser.validConditions(perso, group.getCondition()) ||( group.capturer != perso.getId() && (group._creationDate + 90 * 60 * 1000) > System.currentTimeMillis())) {
            SocketManager.GAME_SEND_Im_PACKET(perso, "119");
            return;
        }

    final Party party=perso.getParty();

    if(party!=null&&party.getMaster()!=null&&!party.getMaster().getName().equals(perso.getName())&&party.isWithTheMaster(perso,false)) {
   	 perso.sendMessage("Personnage non maitre.");
     return;
   }
    if(party!=null&&party.getChief()!=null&&!party.getChief().getName().equals(perso.getName())&&party.isWithTheMaster3(perso,false)) {
     	 perso.sendMessage("(<b>Groupe</b>) : Vous  tes sur la m me map que votre chef de groupe, que celui-ci peut lancer des combats.");
        return;
      }
  
   try {
   Main.world.getCryptManager().parseStartCell(this,0);
   Main.world.getCryptManager().parseStartCell(this,1);
   }catch (Exception e) {
   	 perso.sendMessage("Cette carte n'a pas de positions de combat, veuillez poster le mapID (type / mapid) et ce message dans le canal de rapport de bug sur <b><a href='"+Config.getInstance().discord+"'>Discord</a></b>.");
   	return;
   }
    perso.itemchek();
    int id=getFightID();
    boolean mode = perso.DonjonModulaire;
    boolean modduo = perso.isDuo();
    if (!Constant.Donjonsmaps.contains(String.valueOf(perso.getCurMap().getId()))) {
        mode = false;  
    }
    if (Capture.isInDjRocheMap(perso.getCurMap().getId()) && perso.DonjonModulaire) {
        mode = false; 
        perso.sendMessage("<b>(Modulaire)</b> : Votre mode Donjon Modulaire a  t  d sactiv  dans cette zone.");
    }
    if (perso.getCurMap().getSong() == 1 && perso.DonjonModulaire) {
        mode = false; 
        perso.sendMessage("<b>(Modulaire)</b> : Votre mode Donjon Modulaire a  t  d sactiv  dans cette zone.");
    }
    if (Capture.isInEventMap(perso.getCurMap().getId()) && perso.isDuo()) {
        modduo = false; 
        perso.sendMessage("<b>(Modulaire)</b> : Votre mode DUO a  t  d sactiv  dans cette zone.");
    }
      if (Capture.isInArenaMap(perso.getCurMap().getId()) && perso.DonjonModulaire) {
        mode = false; 
      perso.sendMessage("<b>(Modulaire)</b> : Votre mode Modulaire a  t  d sactiv  dans cette zone.");
      }
    Fight fight=new Fight(id,this,perso,group,mode);
    if(fight == null) {
    	SocketManager.GAME_SEND_MESSAGE(perso,"You can not join this fight as it is forbidden.");
        return;
    }
    mobGroups.remove(group.getId());
    perso.setOldMap(perso.getCurMap().getId());
    if(perso.getCurCell() == null)
    perso.setCurCell(perso.getCurMap().getCase(perso.getCurMap().getRandomFreeCellId()));	
    perso.setOldCell(perso.getCurCell().getId());
    fights=new ArrayList<>();
    fights.add(fight);
    SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(this);
	/*if(perso.getCurMap().getSong() == 0)
		perso.Chek_item_boutique();*/ // Pour songe
    if (party == null) { 
	    TimerWaiterPlus.addNext(() -> { 
	    	if (perso.isSoloReady() && perso.getCurMap().getId() != 16072 && perso.getCurMap().getId() != 16073 && perso.getCurMap().getId() != 16074) {
	    
    //	perso.setReady(true); // mettre booléen prêt 
      //  SocketManager.GAME_SEND_FIGHT_PLAYER_READY_TO_FIGHT(perso.getFight(), 3, perso.getId(), true);
  		perso.getAccount().getGameClient().readyFight("GR1");

          perso.sendMessage("<b>(Informations)</b> : Le mode <b>SoloReady</b> est activé, donc votre combat a commencé.");
	    	}},150, TimerWaiterPlus.DataType.CLIENT);
    }
    if(party!=null && perso.getParty().isChief(perso.getId()))
    {
    	TimerWaiterPlus.addNext(() ->
    	party.getPlayers().stream().forEach(follower -> {
    		if (follower.isReadyFarm() && perso.getCurMap().getId() != 16072 && perso.getCurMap().getId() != 16073 && perso.getCurMap().getId() != 16074) {
      			follower.setReady(true); // mettre bool en pr t 
                SocketManager.GAME_SEND_FIGHT_PLAYER_READY_TO_FIGHT(follower.getFight(), 3, follower.getId(), true);
			follower.getAccount().getGameClient().readyFight("GR1");
    		}
      }),700, TimeUnit.MILLISECONDS, TimerWaiterPlus.DataType.MAP);
    }
      if(party!=null)
      {
      	TimerWaiterPlus.addNext(() ->
      	party.getPlayers().stream().forEach(follower -> {
      		if(follower.getFight() == null)
      		if(follower.getCurMap().id == perso.getCurMap().id)	
          if(fight.getPrism()!=null)
            fight.joinPrismFight(follower,(fight.getTeam0().containsKey(perso.getId()) ? 0 : 1));
          else
        	  if(follower.getFight() == null)
            		if(follower.getCurMap().id == perso.getCurMap().id)
      		if(follower != null)
            fight.joinFight(follower,perso.getId());
        }),0, TimeUnit.MILLISECONDS, TimerWaiterPlus.DataType.MAP);
      }
     
    }



  //v2.8 - New Fight ID
  public synchronized short getFightID()
  {
    if(fights==null)
      fights=new ArrayList<>();
    if(fights.isEmpty())
      return 1;
    for(short id=1;true;id++)
    {
      if(fights.size()<id)
        return id;
      if(fights.get(id-1)==null)
        return id;
    }
  }

  //v2.7 - Tactical mode memory
  public void startFightVersusProtectors(Player perso, MobGroup group)
  {
    if(perso.getFight()!=null)
      return;
    if(Config.getInstance().fightAsBlocked)
      return;
    if(perso.isDead()==1)
      return;
    if(!perso.canAggro())
      return;
    int id=1;

    if(this.placesStr.isEmpty()||this.placesStr.equals("|"))
    {
      perso.sendMessage("Cette carte n'a pas de positions de combat, veuillez poster le mapID (type / mapid) et ce message dans le canal de rapport de bug sur <b><a href='"+Config.getInstance().discord+"'>Discord</a></b>.");
      return;
    }
    try {
    Main.world.getCryptManager().parseStartCell(this,0);
    Main.world.getCryptManager().parseStartCell(this,1);
    }catch (Exception e) {
    	 perso.sendMessage("Cette carte n'a pas de positions de combat, veuillez poster le mapID (type / mapid) et ce message dans le canal de rapport de bug sur <b><a href='"+Config.getInstance().discord+"'>Discord</a></b>.");
    	return;
    }
	/*if(perso.getCurMap().getSong() == 0)
		perso.Chek_item_boutique();*/ // Pour songe
    perso.itemchek();
    if(this.fights==null)
      this.fights=new ArrayList<>();
    if(!this.fights.isEmpty())
      id=((Fight)(this.fights.toArray()[this.fights.size()-1])).getId()+1;
    this.fights.add(new Fight(id,this,perso,group,Constant.FIGHT_TYPE_PVM,true));
    SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(this);
  }

  //v2.7 - Tactical mode memory
  public void startFigthVersusDopeuls(Player perso, MobGroup group) //RaZoR
  {
    if(perso.getFight()!=null||Config.getInstance().fightAsBlocked||perso.isDead()==1||!perso.canAggro()||perso.afterFight)
      return;
    if(this.placesStr.isEmpty()||this.placesStr.equals("|"))
    {
      perso.sendMessage("Cette carte n'a pas de positions de combat, veuillez poster le mapID (type / mapid) et ce message dans le canal de rapport de bug sur <b><a href='"+Config.getInstance().discord+"'>Discord</a></b>.");
      return;
    }
    try {
    Main.world.getCryptManager().parseStartCell(this,0);
    Main.world.getCryptManager().parseStartCell(this,1);
    }catch (Exception e) {
    	 perso.sendMessage("Cette carte n'a pas de positions de combat, veuillez poster le mapID (type / mapid) et ce message dans le canal de rapport de bug sur <b><a href='"+Config.getInstance().discord+"'>Discord</a></b>.");
    	return;
    }
    perso.itemchek();
    int id=1;
    if(this.fights==null)
      this.fights=new ArrayList<>();
    if(!this.fights.isEmpty())
      id=((Fight)(this.fights.toArray()[this.fights.size()-1])).getId()+1;
    this.fights.add(new Fight(id,this,perso,group,Constant.FIGHT_TYPE_DOPEUL,true));
    SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(this);
  }

  //v2.7 - Tactical mode memory
  public void startFightVersusPercepteur(Player perso, Collector perco)
  {
    if(perso.getFight()!=null)
      return;
    if(Config.getInstance().fightAsBlocked)
      return;
    if(perso.isDead()==1)
      return;
    if(!perso.canAggro())
      return;
    perso.itemchek();
    int id=1;
    try {
    Main.world.getCryptManager().parseStartCell(this,0);
    Main.world.getCryptManager().parseStartCell(this,1);
    }catch (Exception e) {
    	 perso.sendMessage("Cette carte n'a pas de positions de combat, veuillez poster le mapID (type / mapid) et ce message dans le canal de rapport de bug sur <b><a href='"+Config.getInstance().discord+"'>Discord</a></b>.");
    	return;
    }
	/*if(perso.getCurMap().getSong() == 0)
		perso.Chek_item_boutique();*/ // Pour songe
    if(this.fights==null)
      this.fights=new ArrayList<>();
    if(!this.fights.isEmpty())
      id=((Fight)(this.fights.toArray()[this.fights.size()-1])).getId()+1;

    this.fights.add(new Fight(id,this,perso,perco));
    SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(this);
  }

  //v2.7 - Tactical mode memory
  public void startFightVersusPrisme(Player perso, Prism Prisme)
  {
    if(perso.getFight()!=null)
      return;
 
    try {
    Main.world.getCryptManager().parseStartCell(this,0);
    Main.world.getCryptManager().parseStartCell(this,1);
    }catch (Exception e) {
    	 perso.sendMessage("Cette carte n'a pas de positions de combat, veuillez poster le mapID (type / mapid) et ce message dans le canal de rapport de bug sur <b><a href='"+Config.getInstance().discord+"'>Discord</a></b>.");
    	return;
    }
    if(Config.getInstance().fightAsBlocked)
      return;
    if(perso.isDead()==1)
      return;
    if(!perso.canAggro())
      return;
    int id=1;
    perso.itemchek();
    if(this.fights == null)
    	return;
    if(!this.fights.isEmpty())
      id=((Fight)(this.fights.toArray()[this.fights.size()-1])).getId()+1;
    this.fights.add(new Fight(id,this,perso,Prisme));
    SocketManager.SEND_GA_ACTION_TO_Map(perso.getCurMap(),"",909,perso.getId()+"",id+"");
    SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(this);
	/*if(perso.getCurMap().getSong() == 0)
		perso.Chek_item_boutique();*/ // Pour songe
    for(Player player : Main.world.getOnlinePlayers())
    {
      if(player==null)
        continue;
      player.sendMessage("Prisme : ["+pos+"] a ete attaqué.");
  	
    }
  }

  public int getRandomFreeCellId()
  {
    ArrayList<Integer> freecell=new ArrayList<>();

    for(GameCase entry : cases)
    {
      if(entry==null)
        continue;
      if(!entry.isWalkable(true))
        continue;
      if(entry.getObject()!=null)
        continue;
      if(this.id==8279)
      {
        switch(entry.getId())
        {
          case 86:
          case 100:
          case 114:
          case 128:
          case 142:
          case 156:
          case 170:
          case 184:
          case 198:
            continue;
        }
      }
      if(this.mountPark!=null)
        if(this.mountPark.getCellOfObject().contains((int)entry.getId()))
          continue;

      boolean ok=true;
      if(this.mobGroups!=null)
        for(MobGroup mg : this.mobGroups.values())
          if(mg!=null)
            if(mg.getCellId()==entry.getId())
              ok=false;
      if(this.npcs!=null)
        for(Npc npc : this.npcs.values())
          if(npc!=null)
            if(npc.getCellid()==entry.getId())
              ok=false;

      if(!ok||!entry.getPlayers().isEmpty())
        continue;
      freecell.add(entry.getId());
    }

    if(freecell.isEmpty())
      return -1;
    return freecell.get(Formulas.getRandomValue(0,freecell.size()-1));
  }

  public int getRandomNearFreeCellId(int cellid) //obtenir une cell alï¿½atoire et proche
  {
    ArrayList<Integer> freecell=new ArrayList<>();
    ArrayList<Integer> cases=new ArrayList<>();

    cases.add((cellid+1));
    cases.add((cellid-1));
    cases.add((cellid+2));
    cases.add((cellid-2));
    cases.add((cellid+14));
    cases.add((cellid-14));
    cases.add((cellid+15));
    cases.add((cellid-15));
    cases.add((cellid+16));
    cases.add((cellid-16));
    cases.add((cellid+27));
    cases.add((cellid-27));
    cases.add((cellid+28));
    cases.add((cellid-28));
    cases.add((cellid+29));
    cases.add((cellid-29));
    cases.add((cellid+30));
    cases.add((cellid-30));
    cases.add((cellid+31));
    cases.add((cellid-31));
    cases.add((cellid+42));
    cases.add((cellid-42));
    cases.add((cellid+43));
    cases.add((cellid-43));
    cases.add((cellid+44));
    cases.add((cellid-44));
    cases.add((cellid+45));
    cases.add((cellid-45));
    cases.add((cellid+57));
    cases.add((cellid-57));
    cases.add((cellid+58));
    cases.add((cellid-58));
    cases.add((cellid+59));
    cases.add((cellid-59));

    for(int entry : cases)
    {
      GameCase gameCase=this.getCase(entry);
      if(gameCase==null)
        continue;
      if(gameCase.getOnCellStopAction())
        continue;
      //Si la case n'est pas marchable
      if(!gameCase.isWalkable(true))
        continue;
      //Si la case est prise par un groupe de monstre
      boolean ok=true;
      for(Entry<Integer, MobGroup> mgEntry : this.mobGroups.entrySet())
        if(mgEntry.getValue().getCellId()==gameCase.getId())
          ok=false;
      if(!ok)
        continue;
      //Si la case est prise par un npc
      ok=true;
      for(Entry<Integer, Npc> npcEntry : this.npcs.entrySet())
        if(npcEntry.getValue().getCellid()==gameCase.getId())
          ok=false;
      if(!ok)
        continue;
      //Si la case est prise par un joueur
      if(!gameCase.getPlayers().isEmpty())
        continue;
      //Sinon
      freecell.add(gameCase.getId());
    }
    if(freecell.isEmpty())
      return -1;
    int rand=Formulas.getRandomValue(0,freecell.size()-1);
    return freecell.get(rand);
  }

  public void onMapMonsterDeplacement()
  {
    if(getMobGroups().size()==0)
      return;
    int RandNumb=Formulas.getRandomValue(1,getMobGroups().size());
    int i=0;
    for(MobGroup group : getMobGroups().values())
    {
      switch(this.id)
      {
        case 8279:// W:15   H:17
          final int cell1=group.getCellId();
          final GameCase cell2=this.getCase((cell1-15)),cell3=this.getCase((cell1-15+1));
          final GameCase cell4=this.getCase((cell1+15-1)),cell5=this.getCase((cell1+15));
          boolean case2=(cell2!=null&&(cell2.isWalkable(true)&&(cell2.getPlayers().isEmpty())));
          boolean case3=(cell3!=null&&(cell3.isWalkable(true)&&(cell3.getPlayers().isEmpty())));
          boolean case4=(cell4!=null&&(cell4.isWalkable(true)&&(cell4.getPlayers().isEmpty())));
          boolean case5=(cell5!=null&&(cell5.isWalkable(true)&&(cell5.getPlayers().isEmpty())));
          ArrayList<Boolean> array=new ArrayList<>();
          array.add(case2);
          array.add(case3);
          array.add(case4);
          array.add(case5);

          int count=0;
          for(boolean bo : array)
            if(bo)
              count++;

          if(count==0)
            return;
          if(count==1)
          {
            GameCase newCell=(case2 ? cell2 : (case3 ? cell3 : (case4 ? cell4 : cell5)));
            GameCase nextCell=null;
            if(newCell==null)
              return;

            if(newCell.equals(cell2))
            {
              if(checkCell(newCell.getId()-15))
              {
                nextCell=this.getCase(newCell.getId()-15);
                if(this.checkCell(nextCell.getId()-15))
                {
                  nextCell=this.getCase(nextCell.getId()-15);
                }
              }
            }
            else if(newCell.equals(cell3))
            {
              if(this.checkCell(newCell.getId()-15+1))
              {
                nextCell=this.getCase(newCell.getId()-15+1);
                if(this.getCase(nextCell.getId()-15+1)!=null)
                {
                  nextCell=this.getCase(nextCell.getId()-15+1);
                }
              }
            }
            else if(newCell.equals(cell4))
            {
              if(this.checkCell(newCell.getId()+15-1))
              {
                nextCell=this.getCase(newCell.getId()+15-1);
                if(this.checkCell(nextCell.getId()+15-1))
                {
                  nextCell=this.getCase(nextCell.getId()+15-1);
                }
              }
            }
            else if(newCell.equals(cell5))
            {
              if(this.checkCell(newCell.getId()+15))
              {
                nextCell=this.getCase(newCell.getId()+15);
                if(this.checkCell(nextCell.getId()+15))
                {
                  nextCell=this.getCase(nextCell.getId()+15);
                }
              }
            }

            String pathstr;
            try
            {
              pathstr=PathFinding.getShortestStringPathBetween(this,group.getCellId(),nextCell.getId(),0);
            }
            catch(Exception e)
            {
              e.printStackTrace();
              return;
            }
            if(pathstr==null)
              return;
            group.setCellId(nextCell.getId());
            for(Player z : getPlayers())
              SocketManager.GAME_SEND_GA_PACKET(z.getGameClient(),"0","1",group.getId()+"",pathstr);
          }
          else
          {
            if(group.isFix())
              continue;
            i++;
            if(i!=RandNumb)
              continue;

            int cell=-1;
            while(cell==-1||cell==383||cell==384||cell==398||cell==369)
              cell=getRandomNearFreeCellId(group.getCellId());
            String pathstr;
            try
            {
              pathstr=PathFinding.getShortestStringPathBetween(this,group.getCellId(),cell,0);
            }
            catch(Exception e)
            {
              e.printStackTrace();
              return;
            }
            if(pathstr==null)
              return;
            group.setCellId(cell);
            for(Player z : getPlayers())
              SocketManager.GAME_SEND_GA_PACKET(z.getGameClient(),"0","1",group.getId()+"",pathstr);
          }
          break;

        default:
          if(group.isFix())
            continue;
          i++;
          if(i!=RandNumb)
            continue;
          int cell=getRandomNearFreeCellId(group.getCellId());
          String pathstr;
          try
          {
            pathstr=PathFinding.getShortestStringPathBetween(this,group.getCellId(),cell,0);
          }
          catch(Exception e)
          {
            e.printStackTrace();
            return;
          }
          if(pathstr==null)
            return;
          group.setCellId(cell);
          for(Player z : getPlayers())
            SocketManager.GAME_SEND_GA_PACKET(z.getGameClient(),"0","1",group.getId()+"",pathstr);
          break;
      }

    }
  }

  public boolean checkCell(int id)
  {
    return this.getCase(id-15)!=null&&this.getCase(id-15).isWalkable(true);
  }

  public String getObjects()
  {
    if(this.mountPark==null||this.mountPark.getObject().size()==0)
      return "";
    String packets="GDO+";
    boolean first=true;
    for(Entry<Integer, Map<Integer, Integer>> entry : this.mountPark.getObject().entrySet())
    {
      for(Entry<Integer, Integer> entry2 : entry.getValue().entrySet())
      {
        if(!first)
          packets+="|";
        int cellidDurab=entry.getKey();
        packets+=entry.getKey()+";"+entry2.getKey()+";1;"+getObjDurable(cellidDurab);
        first=false;
      }
    }
    return packets;
  }

  public String getObjDurable(int CellID)
  {
    String packets="";
    for(Entry<Integer, Map<Integer, Integer>> entry : this.mountPark.getObjDurab().entrySet())
    {
      for(Entry<Integer, Integer> entry2 : entry.getValue().entrySet())
      {
        if(CellID==entry.getKey())
          packets+=entry2.getValue()+";"+entry2.getKey();
      }
    }
    return packets;
  }

  public boolean cellSideLeft(int cell)
  {
    int ladoIzq=this.w;
    for(int i=0;i<this.w;i++)
    {
      if(cell==ladoIzq)
        return true;
      ladoIzq=ladoIzq+(this.w*2)-1;
    }
    return false;
  }

  public boolean cellSideRight(int cell)
  {
    int ladoDer=2*(this.w-1);
    for(int i=0;i<this.w;i++)
    {
      if(cell==ladoDer)
        return true;
      ladoDer=ladoDer+(this.w*2)-1;
    }
    return false;
  }

  public boolean cellSide(int cell1, int cell2)
  {
    if(cellSideLeft(cell1))
      if(cell2==cell1+(this.w-1)||cell2==cell1-this.w)
        return true;
    if(cellSideRight(cell1))
      if(cell2==cell1+this.w||cell2==cell1-(this.w-1))
        return true;
    return false;
  }

  public String getGMOfMount(boolean ok, Player perso)
  {
    if(this.mountPark==null||this.mountPark.getListOfRaising().size()==0)
      return "";

    ArrayList<Mount> mounts=new ArrayList<>();

    for(Integer id : this.mountPark.getListOfRaising())
    {
      Mount mount=Main.world.getMountById(id);

      if(mount!=null)
        if(mount.getOwner()== perso.getId()||this.mountPark.getGuild()!=null)
          mounts.add(mount);
    }

    if(ok)
      for(Player target : this.getPlayers())
        SocketManager.GAME_SEND_GM_MOUNT(target.getGameClient(),this,false);

    return this.getGMOfMount(mounts);
  }

  public String getGMOfMount(ArrayList<Mount> mounts)
  {
    if(this.mountPark==null||this.mountPark.getListOfRaising().size()==0)
      return "";
    StringBuilder packets=new StringBuilder();
    packets.append("GM|+");
    boolean first=true;
    for(Mount mount : mounts)
    {
      String GM=mount.parseToGM();
      if(GM==null||GM.equals(""))
        continue;
      if(!first)
        packets.append("|+");
      packets.append(GM);
      first=false;
    }

    return packets.toString();
  }

  public Player getPlayer(int id)
  {
    for(GameCase cell : cases)
      for(Player player : cell.getPlayers())
        if(player!=null)
          if(player.getId()==id)
            return player;
    return null;
  }
  public long getTime_pose_perco() {
		return time_pose_perco;
	}

	public void setTime_pose_perco(long time_pose_perco) {
		this.time_pose_perco = time_pose_perco;
	}
  public void onPlayerArriveOnCell(Player player, int id)
  {
    GameCase cell=this.getCase(id);
    if(cell==null)
      return;
    byte cof = (byte) (player.isOnMount() ? 2 : 1);
    if(player != null && player.getGameClient() != null)
    if (player.getGroupe() == null && player.getGameClient().depla != 0 && System.currentTimeMillis() - player.getGameClient().depla < (550 / cof)) {
        player.sendDiscord(Constant.WEBHOOK_SPEEDHACK,"Le joueur " + player.getName() + " vient d'utiliser la faille speedHack Walk. à vérifier !");
       // player.sendMessage(" Test "+120 / cof+" et "+player.getGameClient().depla+"");
    }
   // Long Test = System.currentTimeMillis() - player.getGameClient().depla;
   // player.sendMessage(" Test "+120 / cof+" et "+ Test+"");


    player.setAway(false);
    synchronized(cell)
    {
      GameObject obj=cell.getDroppedItem(true);
      if(obj!=null&&!Config.getInstance().mapAsBlocked)
      {
         Database.getStatics().getPlayerData().logs_ramase(player.getName(), "GetInOnTheFloor : "+player.getName()+" a ramasse ["+obj.getTemplate().getId()+"@"+obj.getGuid()+";"+obj.getQuantity()+"]");
        if(player.addObjet(obj,true))
          World.addGameObject(obj,true);
        SocketManager.GAME_SEND_GDO_PACKET_TO_MAP(this,'-',id,0,0);
        SocketManager.GAME_SEND_Ow_PACKET(player);
      }
      if(obj!=null&&Config.getInstance().mapAsBlocked)
        SocketManager.GAME_SEND_MESSAGE(player,"L'administrateur a temporairement bloqué l'accès aux objets de récolte.");
    }
    InteractiveDoor.check(player,this);
    this.getCase(id).applyOnCellStopActions(player);
    if(this.placesStr.equalsIgnoreCase("|"))
      return;
    if(player.getCurMap().getId()!=this.id||!player.canAggro())
      return;
    for(MobGroup group : this.mobGroups.values())
    {
    	if(this.id == 11095)
    	if(id == 583)
    		id = 509;
      if(PathFinding.getDistanceBetween(this,id,group.getCellId())<=group.getAggroDistance())
      { //S'il y aggr
        startFightVersusMonstres(player,group);
        return;
      }
    }
  }

  public void send(String packet)
  {
    this.getPlayers().stream().filter(player -> player!=null).forEach(player -> player.send(packet));
  }

  public int getMinRespawnTime()
  {
    return minRespawnTime;
  }

  public void setMinRespawnTime(int minRespawnTime)
  {
    this.minRespawnTime=minRespawnTime;
  }

  public int getMaxRespawnTime()
  {
    return maxRespawnTime;
  }

  public void setMaxRespawnTime(int maxRespawnTime)
  {
    this.maxRespawnTime=maxRespawnTime;
  }
  public static boolean isInArenaMap(int currentMapId) {
      return Arrays.asList(10131, 10132, 10133, 10134, 10135, 10136, 10137, 10138).contains(currentMapId);

  }
  public static boolean isInStartMap(int currentMapId) {
      return Arrays.asList(10300, 10284, 10299, 10285, 10298, 10276, 10283, 10294, 10292, 10279, 10296, 10289).contains(currentMapId);

  }
  public static boolean isInCraftMap(int currentMapId) {
      return Arrays.asList(18500, 18501, 10315, 8731, 8732, 10316).contains(currentMapId);

  }
  public void moveMobGroups(int mover)
  {
    // String str = "";
    try
    {
      int cantGruposAMover=0;
      while(cantGruposAMover<mover)
      {
        boolean noHay=true;
        for(final MobGroup grupoMob : mobGroups.values())
        {
          if(grupoMob.getFight()!=null)
          {
            continue;
          }
          if (isInArenaMap(this.getId())) {
        	  continue;
          }
          if(this.getId() == 534 || this.getId() == 2021 || this.getId() == 11095 || this.getId() == 6680)
          continue;
          
          noHay=false;
          if(Formulas.getRandomBoolean())
          {
            grupoMob.moveMobGroup(this);
            cantGruposAMover++;
          }
        }
        if(noHay)
        {
          break;
        }
      }
    }
    catch(final Exception e)
    {
    }
    // return str;
  }

  public void zillaTimer()
  {
	  TimerWaiterPlus.addNext(() -> this.spawnZilla(),3*60*1000, TimerWaiterPlus.DataType.MAP); //3 minutes before spawn
  }

  private void spawnZilla()
  {
    this.spawnGroupWith(Main.world.getMonstre(599));
  }

	public void openKrala()
	{
		SocketManager.SEND_Im1223_ALL("La porte du Kralamour vient d'être ouverte ! Faîtes vite, elle restera ouverte pendant 1 heure.");
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
			}
	public void openKrala05()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala1()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala2()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala3()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala4()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala5()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala6()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala7()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala8()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala9()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala10()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala11()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala12()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala13()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala14()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala15()
	{
		SocketManager.SEND_Im1223_ALL("La porte du Kralamour se fermera dans 45 minutes, faites vite !");
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala16()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala17()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala18()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala19()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala20()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala21()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala22()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala23()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala24()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala25()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala26()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala27()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala28()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala29()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala30()
	{
		SocketManager.SEND_Im1223_ALL("La porte du Kralamour se fermera dans 30 minutes, faites vite !");
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala31()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala32()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala33()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala34()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala35()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala36()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala37()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala38()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala39()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala40()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala41()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala42()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala43()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala44()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala45()
	{
		SocketManager.SEND_Im1223_ALL("La porte du Kralamour se fermera dans 15 minutes, faites vite !");
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala46()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala47()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala48()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala49()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala50()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala51()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala52()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala53()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala54()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala55()
	{
		SocketManager.SEND_Im1223_ALL("La porte du Kralamour se fermera dans 5 minutes, faites vite !");
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala56()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala57()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala58()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}	public void openKrala59()
	{
		SocketManager.GAME_UPDATE_CELL(this, "328;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, true);

		this.cases.get(286).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, true);
		this.cases.get(300).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, true);
		this.cases.get(315).setWalkable(true);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, true);
	}

	
	public void closeKrala()
	{
		SocketManager.SEND_Im1223_ALL("La porte du Kralamour vient d'être fermée ! Elle se fermera pour une durée de 3heures...");
		SocketManager.GAME_UPDATE_CELL(this, "328;aaaaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 328, false);
		cases.get(286).setWalkable(false);
		SocketManager.GAME_UPDATE_CELL(this, "286;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 286, false);
		cases.get(300).setWalkable(false);
		SocketManager.GAME_UPDATE_CELL(this, "300;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 300, false);
		cases.get(315).setWalkable(false);
		SocketManager.GAME_UPDATE_CELL(this, "315;aaGaaaaaaa801;1");
		SocketManager.GAME_SEND_ACTION_TO_DOOR(this, 315, false);
	}

	public void addstar(final int star) {
		for (MobGroup group : mobGroups.values()) {
		    if(group.realBonusStars() >= 200)continue;
            if(group.realBonusStars() == 0 || group.LastBonusStars == 0L){
                if(group._creationDate + (60 * 60 * 1000) < System.currentTimeMillis()){
                    group.addStarBonusAfterTime(group.realBonusStars()+20);
                    group.LastBonusStars  = System.currentTimeMillis();
                }


            }

            else {
                if (group.LastBonusStars != 0) {
                    if (group.LastBonusStars + (group.GetTime() * 60 * 60 * 1000) < System.currentTimeMillis()) {
                        group.addStarBonusAfterTime(group.realBonusStars() + 20);
                        group.LastBonusStars = System.currentTimeMillis();
                    }
                } else {
                    if (group.LastBonusStars + (3 * 60 * 60 * 1000) < System.currentTimeMillis()) {
                        group.addStarBonusAfterTime(group.realBonusStars() + 20);
                        group.LastBonusStars = System.currentTimeMillis();
                    }
                }
             }
            }

        }

	  public void addstarCommand(int star) {
		    for (MobGroup group : this.mobGroups.values())
		      group.AddBonusStarsCommand(star); 
		  }


	    public void borrarNPCoGrupoMob(int id) {
			npcs.remove(id);
			mobGroups.remove(id);
		}

		public void borrarNPC() {
			npcs.clear();
			for (int id : npcs.keySet()) {
				SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this, id);
			}
		}

		public void borrarTodosMobs() {
			mobPossibles.clear();
			for (int id : mobGroups.keySet()) {
				SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this, id);
			}
			mobGroups.clear();
		}

		public void borrarTodosMobsFix() {
			mobExtras.clear();
			for (int id : mobExtras.keySet()) {
				SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this, id);
			}
			// GestorSQL.BORRAR_MOBSFIX_MAPA(id);
			mobGroups.clear();
		}
	    private Map<Integer, Pair<Integer, Integer>> flags = new HashMap<>();
		public String getObjectsGDsPacketsInFight() {
	        StringBuilder toreturn = new StringBuilder();
	        boolean first = true;
	        for(GameCase entry : cases)
	        {
	            if(entry.getObject() != null)
	            {
	                if(!first)toreturn.append((char)0x00);
	                first = false;
	                int cellID = entry.getId();
	                toreturn.append("GDF|").append(cellID).append(";0;0");
	            }
	        }
	        return toreturn.toString();
	    }

		public synchronized String getFightersGMsPackets(Fight fight, Player player) {
	        StringBuilder packet = new StringBuilder("GM");

	        try {
	        for (GameCase cell : this.cases)
	            cell.getFighters().stream().filter(fighter -> fighter.getFight() == fight)
	                    .forEach(fighter -> packet.append("|").append(fighter.getGmPacket('+', false)));
	        } catch (Exception e ){

	        }
	        return packet.toString();

	    }
	    public Map<Integer, Pair<Integer, Integer>> getFlags() {
	        return flags;
	    }

	    public void setFlags(Map<Integer, Pair<Integer, Integer>> flags) {
	        this.flags = flags;
	    }
		
		
}
