/*
 * Created on 16-Jan-2005
 *
 * By Mike Anderson
 */
package mikera.tyrant;

import java.awt.Color;
import java.awt.Graphics;

import mikera.engine.BaseObject;
import mikera.engine.RPG;
import mikera.util.Maths;

/**
 * @author Mike
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Animation extends BaseObject {
	private static final long serialVersionUID = 1L;
    private static final int TEST=0;
	private static final int SHOT=1;
	private static final int EXPLOSION=2;
	private static final int SPARK=3;
	private static final int HIT=4;
	private static final int SEQUENCE=5;
	private static final int UNION=6;
	private static final int DELAY=7;
	private static final int SPRAY=8;
	
	public Animation(int type) {
		set("Type",type);
		set("StartTime",System.currentTimeMillis());
	}

	public static Animation test() {
		return new Animation(TEST);
	}

	
	public static Animation spark(int x, int y, int c) {
		Animation a=new Animation(SPARK);
		a.set("X",x);
		a.set("Y",y);
		a.set("C",c);
		a.set("LifeTime",500);
		return a;
	}
	
	public static Animation hit(int x, int y, int c) {
		Animation a=new Animation(HIT);
		
		// note: we are setting double values here
		a.set("X",x+0.5*(RPG.random()-0.5));
		a.set("Y",y+0.5*(RPG.random()-0.5));
		
		a.set("C",c);
		a.set("LifeTime",200);
		return a;
	}
	
	public static Animation explosion(int x, int y, int c, int r) {
		Animation a=new Animation(EXPLOSION);
		a.set("X",x);
		a.set("Y",y);
		a.set("Radius",r);
		a.set("C",c);
		a.set("LifeTime",500);
		return a;
	}
	
	public static Animation sequence(Animation a1, Animation a2) {
		Animation a=new Animation(SEQUENCE);
		a.set("Animation1",a1);
		a1.set("StartTime",a.getDouble("StartTime"));
		a.set("Animation2",a2);
		return a;
	}
	
	public static Animation union(Animation a1, Animation a2) {
		Animation a=new Animation(UNION);
		a.set("Animation1",a1);
		a1.set("StartTime",a.getDouble("StartTime"));
		a.set("Animation2",a2);
		a2.set("StartTime",a.getDouble("StartTime"));
		return a;
	}
	
	public static Animation delay(int millis) {
		Animation a=new Animation(DELAY);
		a.set("LifeTime",millis);
		return a;
	}
	
	public static Animation shot(int x1, int y1, int x2, int y2, int c, double speed) {
		Animation a=new Animation(SHOT);
		a.set("X1",x1);
		a.set("Y1",y1);
		a.set("X2",x2);
		a.set("Y2",y2);		
		a.set("C",c);
		a.set("LifeTime",Maths.max(1,(int)(1000.0*RPG.dist(x1,y1,x2,y2)/speed)));
		return a;
	}
	
	public static Animation spray(int x1, int y1, int x2, int y2, int c, double speed) {
		Animation a=new Animation(SPRAY);
		a.set("X1",x1);
		a.set("Y1",y1);
		a.set("X2",(x2+RPG.random()-0.5));
		a.set("Y2",(y2+RPG.random()-0.5));		
		a.set("C",c);
		a.set("LifeTime",Maths.max(1,(int)(1000.0*RPG.dist(x1,y1,x2,y2)/speed)));
		return a;
	}
	
	public void draw(MapPanel mp, Graphics g) {
		double start=getDouble("StartTime");
		double now=System.currentTimeMillis();
		double step=now-start;
		
		double progress=0.0; // assume complete
		int life=getStat("LifeTime");
		if (life>0) {
			progress=step/life;
		
			// 10secs max for any animation
			if (life>10000) {
				Game.warn("Animation over 10 seconds long expired");
				progress=1.0;
			}
			
			// expire if complete
			if (progress>=1.0) {
				set("Expired",true);
				return;
			}	
		}
		
		switch (getStat("Type")) {
			case TEST: {
				if (getStat("Count")>100) {
					set("Expired",true);
					break;
				}			
				
				g.setColor(Color.BLUE);
				int n=getStat("N");
				int x=0;
				int y=n*5;
				g.fillRect(x,y,10,(int)(step/100));
				set("N",(n+1)%10);
				incStat("Count",1);

				break;
			}
				
			case SPARK: {
				int x=getStat("X");
				int y=getStat("Y");
				int c=getStat("C");
				c=(c/20)*20+(int)(progress*6);
				mp.drawImage(g,x,y,c);
				
				break;
			}
			
			case EXPLOSION: {
				int x=getStat("X");
				int y=getStat("Y");
				int r=getStat("Radius");
				int c=getStat("C");
				c=(c/20)*20+(int)(progress*6);
				
				for (int dy = -1; dy <= 1; dy++) {
					for (int dx = -1; dx <= 1; dx++) {
						double ds=((dx==0)||(dy==0))?1.0:1.414;
						double tx=x + (r*progress*dx / ds);
						double ty=y + (r*progress*dy / ds);
						mp.drawImage(g,tx,ty,c);
					}
				}
				
				break;
			}
			
			case SHOT: {
				int x1=getStat("X1");
				int y1=getStat("Y1");
				int x2=getStat("X2");
				int y2=getStat("Y2");
				int c=getStat("C");
				
				mp.drawImage(g,x1+progress*(x2-x1),y1+progress*(y2-y1),c);
				
				break;
			}
			
			case HIT: {
				double x=getDouble("X");
				double y=getDouble("Y");
				int c=getStat("C");
				
				mp.drawImage(g,x,y,c);
				
				break;
			}
			
			case SEQUENCE: {
				Animation a1=(Animation)get("Animation1");
				a1.draw(mp,g);
				if (a1.isExpired()) {
					Animation a2=(Animation)get("Animation2");
					set("Animation1",a2);
					set("Animation2",null);
					if (a2==null) {
						set("Expired",true);
					} else {
						a2.set("StartTime",now);
					}
				}
				
				break;
			}
			
			case UNION: {
				Animation a1=(Animation)get("Animation1");
				Animation a2=(Animation)get("Animation2");
				
				boolean expired=true;
				if (!a1.isExpired()) {
					a1.draw(mp,g);
					expired=a1.isExpired();				
				}
				if (!a2.isExpired()) {
					a2.draw(mp,g);
					expired=a2.isExpired();				
				}
				
				set("Expired",expired);

				break;
			}
			
			case SPRAY: {
				int x1=getStat("X1");
				int y1=getStat("Y1");
				double x2=getDouble("X2");
				double y2=getDouble("Y2");
				int c=getStat("C");
				
				mp.drawImage(g,x1+progress*(x2-x1),y1+progress*(y2-y1),(c/20)*20+(int)(progress*6));
				
				break;
			}
			
			
			case DELAY: {
				// do nothing
			}
		}
	}
	
	boolean isExpired() {
		return getFlag("Expired");
	}
}
