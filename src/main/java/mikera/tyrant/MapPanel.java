package mikera.tyrant;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;

import mikera.tyrant.author.Designer;
import mikera.tyrant.engine.Map;
import mikera.tyrant.engine.Point;
import mikera.tyrant.engine.Thing;
import mikera.util.Maths;

// Panel descendant used to display a Map
// contains most of graphics redraw logic
// also some animation/explosion handling
public class MapPanel extends Panel implements Runnable {
	private final class MyMouseListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
        	Thing h=Game.hero();
        	
        	int dir = (e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK
        			? -1
        			: 1;

        	Rectangle rect = getBounds();
        	int rw = rect.width;
        	int rh = rect.height;
        	int x = (e.getX() - (rw - width * TILEWIDTH) / 2) / TILEWIDTH
        			+ scrollx;
        	int y = (e.getY() - (rh - height * TILEHEIGHT) / 2)
        			/ TILEHEIGHT + scrolly;

        	if (dir == 1) {
        	    int rx = x - h.x;
        	    int ry = y - h.y;
        	    if ((rx == 0) && (ry == 0))
        	        return;
        	    int dx = -1;
        	    if (ry < (2 * rx))
        	        dx++;
        	    if (ry > (-2 * rx))
        	        dx++;
        	    int dy = -1;
        	    if (rx < (2 * ry))
        	        dy++;
        	    if (rx > (-2 * ry))
        	        dy++;
        	    Game.simulateDirection(dx, dy);
        	} else {
                if (Game.isDebug()) {
                    map.addThing(h, x, y);
                    Game.warn("Teleport to (" + x + "," + y + ")");
                    map.calcVisible(h,Being.calcViewRange(h));
                    render();
                    repaint();
                }
            }
        	h.calculateVision();
        	viewPosition(map,h.x,h.y);
        }
    }

    private static final long serialVersionUID = 3616728257933161270L;
    // tile size in pixels
	public final static int TILEWIDTH = 32;
	public final static int TILEHEIGHT = 32;
	
	// 3/4 perspective slant
	// TODO: implement changes if there is interest
	private boolean slant=false;
	
	// size of viewable area
	@SuppressWarnings("all")
	protected int width = (TILEWIDTH==32)?15:25;
	@SuppressWarnings("all")
	protected int height = (TILEWIDTH==32)?15:25;

	// zoom factor
	public int zoomfactor = 100;
	private int lastzoomfactor = 100;

	// back buffer
	private Graphics buffergraphics;
	private Image buffer;
	
	// animation buffer
	private Graphics animationgraphics;
	private Image animationbuffer;

	// which map to draw
	public Map map = new Map(5, 5);

	// viewing state
	protected int scrollx = 0;
	protected int scrolly = 0;
	public int curx = 0;
	public int cury = 0;
	public boolean cursor = false;

	// drawing fields
	public int currentTile=Tile.CAVEWALL;
	
	private boolean animating=false;
	private boolean animationdone=false;
	
    public void run() {
    	while (true) {
    	    try {
        	    if (animating) {
        	    	animationdone=false;
        	    	repaint();
        	    }
        	    Thread.sleep(50);
    	    } catch (InterruptedException e) {
    	    	e.printStackTrace();
    	    }
    	}
    }
	
    public void addAnimation(Animation a) {
    	synchronized (animationElements) {
	    	if (!animating) {
	    		animating=true;
	    		// Game.warn("Animation start");
	    	}
	    	animationElements.add(a);
	    }
    }
    
	public MapPanel(GameScreen owner) {
		super();
		
		Thread animloop=new Thread(this);
		animloop.start();
		
		addKeyListener(owner.questapp.keyadapter);

		setBackground(Color.black);
        if(!Game.instance().isDesigner()) addMouseListener(new MyMouseListener());
	}

	// sets current scroll position and repaints map
	public void viewPosition(Map m,int x, int y) {
		setPosition(m,x, y);
		repaint();
	}

    public void scroll(int xDelta, int yDelta) {
        scrollx = Math.min(Math.max(0, xDelta + scrollx), map.width - width / 2);
        scrolly = Math.min(Math.max(0, yDelta + scrolly), map.height - height / 2);
        render();
        repaint();
    }
    
	public void setPosition(Map m, int x, int y) {
		map = m;
		if (map!=null) {
			scrollx = Maths.middle(0, x - width / 2, map.getWidth() - width);
			scrolly = Maths.middle(0, y - height / 2, map.getHeight() - height);
		} 
	}

	//override update to stop flicker
	public void update(Graphics g) {
		paint(g);
	}

	public Dimension getPreferredSize() {
		return new Dimension(width * TILEWIDTH, height * TILEHEIGHT);
	}

	// draws tiles in box (x1,y1)-->(x2,y2) to back buffer
	public void drawTiles(int x1, int y1, int x2, int y2) {
		//Game.warn("("+x1+","+y1+")-("+x2+","+y2+")");
		

		//draw the specified area in buffer
		for (int y = y1; y <= y2; y++) {
			for (int x = x1; x <= x2; x++) {
				drawTile(x, y);
			}
		}
	}

	// draws tile at (x,y)
	// includes logic for mapping tile number to image
	// draw blank if outside map area
	public void drawTile(int x, int y) {
		Thing h=Game.hero();
		
		int m = map.getTile(x, y);

		if (!map.isDiscovered(x, y))
			m = 0;
		Image source = map.isVisible(x, y)
				? QuestApp.tiles
				: QuestApp.greytiles;

		int tile = m & 65535;
		
		int px = (x - scrollx) * TILEWIDTH ;
		int py = (y - scrolly) * TILEHEIGHT;

		if (slant) {
			py=(py*3)/4;
		}
		
		switch (tile) {
			case 0 : //blank
				buffergraphics.setColor(Color.black);
				buffergraphics.fillRect((x - scrollx) * TILEWIDTH,
						(y - scrolly) * TILEHEIGHT, TILEWIDTH, TILEHEIGHT);
				break;

			// default method
			default : {
				int image;
				if (map.isDiscovered(x, y + 1)
						&& Tile.filling[map.getTile(x, y + 1) & 65535]) {
					image = Tile.imagefill[tile];
				} else {
					image = Tile.images[tile];
				}

				int sx = (image % 20) * TILEWIDTH;
				int sy = (image / 20) * TILEHEIGHT;
				
				int EFFECTIVEHEIGHT=TILEHEIGHT;
				if (slant&&!Tile.isFilling(tile)) {
					EFFECTIVEHEIGHT=(EFFECTIVEHEIGHT*3)/4;
					sy+=TILEHEIGHT/4;
					py+=TILEHEIGHT/4;
				}
				
				buffergraphics.drawImage(source, px, py, px + TILEWIDTH, py + EFFECTIVEHEIGHT, 
						sx,	sy, sx + TILEWIDTH, sy + EFFECTIVEHEIGHT, null);

				// draw in coastlines
				if ((Tile.borders[tile] > 0) && (source == QuestApp.tiles)) {
					if ((x > 0) && (map.getTile(x - 1, y) != m))
						buffergraphics.drawImage(QuestApp.scenery, px, py,
								px + TILEWIDTH, py + TILEHEIGHT, 
								0, 16*TILEHEIGHT, TILEWIDTH, 17*TILEHEIGHT, null);
					if ((x < (map.width - 1)) && (map.getTile(x + 1, y) != m))
						buffergraphics.drawImage(QuestApp.scenery, px, py,
								px + TILEWIDTH, py + TILEHEIGHT, 
								TILEWIDTH, 16*TILEHEIGHT, 2*TILEWIDTH, 17*TILEHEIGHT, null);
					if ((y > 0) && (map.getTile(x, y - 1) != m))
						buffergraphics.drawImage(QuestApp.scenery, px, py,
								px + TILEWIDTH, py + TILEHEIGHT, 
								2*TILEWIDTH, 16*TILEHEIGHT, 3*TILEWIDTH, 17*TILEHEIGHT, null);
					if ((y < (map.height - 1)) && (map.getTile(x, y + 1) != m))
						buffergraphics.drawImage(QuestApp.scenery, px, py,
								px + TILEWIDTH, py + TILEHEIGHT, 
								3*TILEWIDTH, 16*TILEHEIGHT, 4*TILEWIDTH, 17*TILEHEIGHT, null);
				}
				break;
			}
		//end of switch
		}

		if (map.isVisible(x, y)) {
			drawThings(x, y);
		} else if ((x==h.x)&&(y==h.y)) {
			drawThing(x,y,h);
		}
	}

	private void drawThing(int x, int y, Thing t) {
		Image im = null;
		if (Game.isDebug()||!t.isInvisible()) {
			int i = t.getImage();
			int sx = (i % 20) * TILEWIDTH;
			int sy = (i / 20) * TILEHEIGHT;
			
			int px = (x - scrollx) * TILEWIDTH;
			int py = (y - scrolly) * TILEHEIGHT;
			if (slant) {
				py=(py*3)/4;
			}
			
			Object source = t.get("ImageSource");
			if (source == null) {
				im = QuestApp.items; // default
			} else {
				im = QuestApp.images.get(source);
			}
			
			buffergraphics.drawImage(im, px, py, px + TILEWIDTH, py + TILEHEIGHT, sx, sy,
					sx + TILEWIDTH, sy + TILEHEIGHT, null);
			if (Game.instance().isDesigner()) {
			    if (t.getFlag("AuthorIsTyped")) {
			        Image overlay = Designer.getOverlayImage();
			        if (overlay != null) {
                        int overlayWidth = overlay.getWidth(null);
                        int overlayHeight = overlay.getHeight(null);
			            buffergraphics.drawImage(overlay, px + TILEWIDTH - overlayWidth, 
                            py, px + TILEWIDTH, py + overlayHeight, 0, 0, overlayWidth, overlayHeight, null);
			        }
			    }
			}
			
			if (t.getFlag("IsBeing")) {
				double health=Being.getHealth(t);
				Color c=new Color(0,0,0,64);
				
				buffergraphics.setColor(c);
				buffergraphics.fillRect(px+20,py+0,12,3);
				
				c=t.isHostile(Game.hero())?Color.RED:Color.GREEN;
				c=new Color(c.getRed(),c.getGreen(),c.getBlue(),128);
				buffergraphics.setColor(c);

				buffergraphics.fillRect(px+21,py+1,(int)(10*health),1);
			}
		}		
	}
	
	// Draw all visible objects on map to back buffer
	// side effect: sorts map objects in increasing z-order
	private void drawThings(int x, int y) {
		Thing head = map.sortZ(x, y);
        int numberOfThings = 0;
		if (head == null)
			return;
		
		do {
			drawThing(x,y,head);
			head = head.next;
            numberOfThings++;
		} while (head != null);
        
		// draw plus icon for designer
		if(numberOfThings > 1) {
            Image plus = Designer.getPlusImage();
            if (plus != null) {
    			int px = (x - scrollx) * TILEWIDTH;
    			int py = (y - scrolly) * TILEHEIGHT;
                int plusWidth = plus.getWidth(null);
                int plusHeight = plus.getHeight(null);
                buffergraphics.drawImage(plus, px + TILEWIDTH - plusWidth, py, px + TILEWIDTH, py + plusHeight, 0, 0, plusWidth, plusHeight, null);
            }
        }
	}
	
    // place cursor at specified position
	public void setCursor(int x, int y) {
		cursor = true;
		curx = x;
		cury = y;
		repaint();
	}

	// remove cursor from map
	public void clearCursor() {
		cursor = false;
		repaint();
	}


	
	public void drawImage(Graphics g, double x, double y, int image) {
		// only draw if the drawn area is visible
		if (!map.isVisible((int)Math.round(x), (int)Math.round(y))) {
			return;
		}
		
		// calculate target position in pixels
		int px = (int)((x - scrollx) * TILEWIDTH);
		int py = (int)((y - scrolly) * TILEHEIGHT);
		
		// calculate source position in pixel, using image index into sprite sheet
		int sx = (image%20) * TILEWIDTH;
		int sy = TILEHEIGHT * (image/20);
		
		g.drawImage(QuestApp.effects, px, py, px + TILEWIDTH,
				py + TILEHEIGHT, sx, sy, sx + TILEWIDTH, sy + TILEHEIGHT,
				null);
	}

	// simple explosion
	public void doExplosion(int x, int y, int c, int dam, String damtype) {
		Game.instance().doExplosion(x, y, c, 1);
		map.areaDamage(x, y, 2, dam, damtype);
	}

	// draws cursor at given location to buffer
	public void drawCursor(int x, int y) {
		int px = (x - scrollx) * TILEWIDTH;
		int py = (y - scrolly) * TILEHEIGHT;
		int sx = 6 * TILEWIDTH;
		int sy = 0 * TILEHEIGHT;
		buffergraphics.drawImage(QuestApp.effects, px, py, px + TILEWIDTH, py
				+ TILEHEIGHT, sx, sy, sx + TILEWIDTH, sy + TILEHEIGHT, null);
	}

	// draw buffer to screen in correct location
	public void drawMap(Graphics g) {
		Image source=buffer;
		
		if (animating&&(animationbuffer!=null)) {
			source=animationbuffer;
		}
		
		Rectangle rect = getBounds();
		int w = rect.width;
		int h = rect.height;
		//g.drawImage(buffer,(w-width*TILEWIDTH*zoomfactor)/2,(h-height*TILEHEIGHT*zoomfactor)/2,null);
		g.drawImage(source, (w - width * TILEWIDTH * zoomfactor / 100) / 2,
				(h - height * TILEHEIGHT * zoomfactor / 100) / 2, width
						* TILEWIDTH * zoomfactor / 100, height * TILEHEIGHT
						* zoomfactor / 100, null);
	}

	public void render() {
		// System.out.println("MapPanel.render()");

		// create back buffer if needed
		if (buffer == null) {
			buffer = createImage(width * TILEWIDTH, height * TILEHEIGHT);
			if (buffer==null) return;
			buffergraphics = buffer.getGraphics();
			animationbuffer = createImage(width * TILEWIDTH, height * TILEHEIGHT);
			animationgraphics = animationbuffer.getGraphics();		
		}

		// draw area to back buffer
		drawTiles(scrollx, scrolly, scrollx + width - 1, scrolly + height
				- 1);
		if (cursor)
			drawCursor(curx, cury);
	}
	
	public void renderAnimation() {
		if (buffer!=null) {
			animationgraphics.drawImage(buffer,0,0,null);
			drawAnimationFrame(animationgraphics);
		}		
	}

	// standard paint method
	// - builds map image in back buffer then copies to screen
	public void paint(Graphics g) {
		// System.out.println("MapPanel.Paint()");
		
		Rectangle rect = getBounds();
		if (zoomfactor != lastzoomfactor) {
			int w = rect.width;
			int h = rect.height;
			g.setColor(Color.black);
			g.fillRect(0, 0, w, h);
			lastzoomfactor = zoomfactor;
		}
		
		if (animating&&(!animationdone)) {
			renderAnimation();
		} 
		
		if (buffer!=null) {
			drawMap(g);
		}
	}
	
	private ArrayList<Animation> animationElements=new ArrayList<>();
	
	public void drawAnimationFrame(Graphics g) {
		synchronized(animationElements) {
			Iterator<Animation> it=animationElements.iterator();
			while(it.hasNext()) {
				Animation ae=it.next();
				ae.draw(this, g);
				
				// remove finished animation parts
				if (ae.isExpired()) {
					it.remove();
				}
			}
			if (animationElements.size()==0) {
				// Game.warn("Animation stop");
				animating=false;
			}
		}
	}
    
     public Point convertUICoordinatesToMap(MouseEvent e) {
         int rw = getWidth();
         int rh = getHeight();
         int x = (e.getX() - (rw - width * MapPanel.TILEWIDTH) / 2) / MapPanel.TILEWIDTH + scrollx;
         int y = (e.getY() - (rh - height * MapPanel.TILEHEIGHT) / 2) / MapPanel.TILEHEIGHT + scrolly;
         return new Point(x, y);
     }
}