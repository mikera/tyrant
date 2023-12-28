// This is the root object for all Quests that can be assigned to the hero
//
// Use as follows:
//   Game.hero.addThing(new Quest(Quest.SOMEQUEST));

package mikera.tyrant;

import java.util.*;

import mikera.tyrant.engine.Lib;
import mikera.tyrant.engine.Map;
import mikera.tyrant.engine.Script;
import mikera.tyrant.engine.Thing;
import mikera.tyrant.util.Text;

public class Quest {

	// get all quests currently active
	public static ArrayList<Thing> getQuests() {
		if (Game.hero()==null) return null;
		
		@SuppressWarnings("unchecked")
		ArrayList<Thing> qs=(ArrayList<Thing>)Game.hero().get("Quests");
		if (qs==null) {
			qs=new ArrayList<>();
			Game.hero().set("Quests",qs);
		}
		
		return qs;
	}
	
	public static String getQuestText(Thing q) {
		StringBuffer sb=new StringBuffer();
		getQuestText(sb,"",q);
		return sb.toString();
	}
	
	private static void getQuestText(StringBuffer sb, String prefix, Thing q) {
		String desc=q.getString("Description");
		if (desc!=null) {
			sb.append(prefix);
			sb.append(Text.capitalise(desc));
			if (q.getFlag("IsComplete")) {
				sb.append(" (complete)");
			}
			if (q.getFlag("IsFailed")) {
				sb.append(" (failed)");
			}
			sb.append("\n");
			
			ArrayList<Thing> qs=getSubQuests(q);
			for (Iterator<Thing> it=qs.iterator(); it.hasNext() ;) {
				Thing sq=it.next();
				getQuestText(sb,prefix+" - ",sq);
			}
		}
	}
	
	public static void addQuest(Thing h, Thing q) {
		@SuppressWarnings("unchecked")
		ArrayList<Thing> qs=(ArrayList<Thing>)h.get("Quests");
		if (qs==null) {
			qs= new ArrayList<>();
			h.set("Quests",qs);
		}
		q.set("Hero",h);
		qs.add(q);
	}

	public static boolean notifyAction(Event actionEvent) {
		return notify(actionEvent);
	}
	
	public static boolean notifyKill(Event killEvent) {
		return notify(killEvent);
	}
	
	private static boolean notify(Event e) {
		ArrayList<Thing> qs=getQuests();
		
		if (qs!=null) for (Iterator<Thing> it=qs.iterator(); it.hasNext();) {
			Thing q=it.next();
			
			if (q.getFlag("IsActive")) {
				q.handle(e);
			}
		}
		return false;
	}
	
	private static ArrayList<Thing> getSubQuests(Thing q) {
		@SuppressWarnings("unchecked")
		ArrayList<Thing> qs=(ArrayList<Thing>)q.get("Quests");
		if (qs==null) qs= new ArrayList<>();
		return qs;
	}
	
	public static void addSubQuest(Thing q, Thing sq) {
		@SuppressWarnings("unchecked")
		ArrayList<Thing> qs=(ArrayList<Thing>)q.get("Quests");
		if (qs==null) {
			qs= new ArrayList<>();
			q.set("Quests",qs);
		}
		sq.set("Parent",q);
		qs.add(sq);
	}
	
	public static void setComplete(Thing q) {
		if (!q.getFlag("IsActive")) {
			throw new Error("Trying to complete a non-active quest");
		}
		q.set("IsComplete",1);
		q.set("IsActive",0);

		if (q.handles("OnQuestComplete")) {
			Event e=new Event("QuestComplete");
			e.set("Quest",q);
			q.handle(e);
		}
		
		Thing parent=q.getThing("Parent");
		if (parent!=null) {
			Event e=new Event("SubQuestComplete");
			e.set("Quest",q);
			q.handle(e);
		}
	}
	
	public static void setFailed(Thing q) {
		if (!q.getFlag("IsActive")) {
			throw new Error("Trying to fail a non-active quest");
		}
		q.set("IsFailed",1);
		q.set("IsActive",0);

		if (q.handles("OnQuestFailed")) {
			Event e=new Event("QuestFailed");
			e.set("Quest",q);
			q.handle(e);
		}
		
		Thing parent=q.getThing("Parent");
		if (parent!=null) {
			Event e=new Event("SubQuestFailed");
			e.set("Quest",q);
			q.handle(e);
		}
	}
	
	public static Thing createKillQuest(String desc,Thing target) {
		Thing t=Lib.create("kill quest");
		t.set("Target",target);
		t.set("Description",desc);
		return t;
	}
	
	public static Thing createKillNumberQuest(String desc,String name,int number) {
		Thing t=Lib.create("kill number quest");
		if (name.startsWith("[")) {
			t.set("TargetType",name.substring(1,name.length()-1));
		} else {
			t.set("TargetName",name);
		}
		t.set("TargetCount",number);
		t.set("Description",desc);
		return t;
	}
	
	public static Thing createMeetQuest(String desc,Thing target) {
		Thing t=Lib.create("meet quest");
		t.set("Target",target);
		t.set("Description",desc);
		return t;
	}
	
	public static Thing createVisitMapQuest(String desc,Map targetMap) {
		Thing t=Lib.create("visit map quest");
		t.set("TargetMap",targetMap);
		t.set("Description",desc);
		return t;
	}
	
	public static Thing createVisitMapQuest(String desc,String targetString) {
		Thing t=Lib.create("visit map quest");
		t.set("TargetMapName",targetString);
		t.set("Description",desc);
		return t;
	}
	
	@SuppressWarnings("serial")
	public static void init() {
		// initialise base quest templates where needed
		
		Thing q;
		
		q=Lib.extend("base quest","base thing");
		q.set("LevelMin",1);
		q.set("IsQuest",1);
		q.set("IsActive",1);
		q.set("IsPhysical",0);
		q.set("Frequency",50);
		q.set("OnQuestComplete",new Script() {
			public boolean handle(Thing q, Event e) {
				String desc=q.getString("Description");
				
				if (desc!=null) {
					Game.message("Quest objective complete: "+desc);
				}
				return false;
			}
		});
		Lib.add(q);
		
		q=Lib.extend("kill quest","base quest");
		q.addHandler("OnKill",new Script() {
			public boolean handle(Thing q, Event e) {
				Thing target=q.getThing("Target");
				
				if ((target!=null)&&target.isDead()) {
					setComplete(q);
				}
				return false;
			}
		});
		Lib.add(q);
		
		q=Lib.extend("kill number quest","base quest");
		q.addHandler("OnKill",new Script() {
			public boolean handle(Thing q, Event e) {
				int target=q.getStat("TargetCount");
				int current=q.getStat("CurrentCount");
					
				// get the thing that was killed
				Thing killed=e.getThing("Target");
				
				String targetName=q.getString("TargetName");
				
				boolean countKill=false;
				
				if (targetName!=null) {
					if (targetName.equals(killed.name())) {
						countKill=true;
					}
				} else {
					String targetType=q.getString("TargetType");
					if ((targetType!=null)&&killed.getFlag(targetType)) {
						countKill=true;
					}
				}
				
				if (countKill) {
					current++;
					q.set("CurrentCount",current);
					
					if (current>=target) {
						setComplete(q);
					}
				}
				
				return false;
			}
		});
		Lib.add(q);
		
		q=Lib.extend("visit map quest","base quest");
		q.addHandler("OnAction",new Script() {
			public boolean handle(Thing q, Event e) {
				Map targetMap=(Map)q.get("TargetMap");
				Map heroMap=Game.hero().getMap();
				// bug 1839105 
				// check to see if hero has already died to
				// avoid null pointer exception
				if (heroMap == null) {
					return false;
				}
				if (targetMap==null) {
					String targetMapName=q.getString("TargetMapName");
					if (heroMap.name().startsWith(targetMapName)) {
						setComplete(q);
					}
				} else {
					if (heroMap==targetMap) {
						setComplete(q);
					}
				}
				return false;
			}
		});
		Lib.add(q);
		
		q=Lib.extend("meet quest","base quest");
		q.addHandler("OnAction",new Script() {
			public boolean handle(Thing q, Event e) {
				Thing target=q.getThing("Target");
				
				Thing h=Game.hero();
				
				if (target.isDead()) {
					setFailed(q);
					return false;
				}
				
				if (h.place!=target.place) return false;

				// check if character is adjacent to hero
				if ((Math.abs(h.x-target.x)<=1)&&(Math.abs(h.y-target.y)<=1)) {
					setComplete(q);
				}
				
				return false;
			}
		});
		Lib.add(q);
		
		q=Lib.extend("sequence quest","base quest");
		Script subQuestScript=new Script() {
			public boolean handle(Thing q, Event e) {
				ArrayList<Thing> sqs=getSubQuests(q);
				
				boolean complete=true;
				boolean failed=true;
				for (Iterator<Thing> it=sqs.iterator(); it.hasNext();) {
					Thing sq=it.next();
					
					if (sq.getFlag("IsFailed")) {
						failed=true;
					}
					
					if (!sq.getFlag("IsComplete")) {
						complete=false;
					}
				}
				
				if (failed) {
					setFailed(q);
				} else if (complete) {
					setComplete(q);
				}
				
				return false;
			}
		};
		q.addHandler("OnSubQuestComplete",subQuestScript);
		q.addHandler("OnSubQuestFailed",subQuestScript);
		Lib.add(q);
	}
}