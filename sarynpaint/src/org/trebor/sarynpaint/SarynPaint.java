/*
 * Copyright (C) 2009 Robert B. Harris (sarynpaint@trebor.org)
 *
 * This file is part of SarynPaint
 *
 * SarynPaint is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Saryn is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SarynPaint.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.trebor.sarynpaint;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.applet.*;
import java.net.*;
import javax.sound.sampled.*;

/**
 * SarynPaint is a simple paint program for very young children.  It was
 * written for my dear friend Saryn who had at the time a particular
 * fondness for the color pink.
 *
 * @author Robert Harris
 */

public class SarynPaint extends JFrame
{
  // constants

  public static int buttonWidth   = 85;
  public static int buttonHeight  = 85;
  public static int alpha         = 128;
  public static float smallScale  = 30;
  public static float mediumScale = 50;
  public static float largeScale  = 80;
  public static float shapeScale  = mediumScale;
  public static int MOVEMODE      = 0;
  public static int STICKYMODE    = 1;
  public static int CLICKMODE     = 2;
  public static int MODECOUNT     = 3;

  // shapes

  public static Shape triangle = createRegularPoly(3);
  public static Shape square   = normalize(new Rectangle2D.Float(0, 0, 1, 1));
  public static Shape pentagon = createRegularPoly(5);
  public static Shape hexagon  = createRegularPoly(6);
  public static Shape circle   = normalize(new Ellipse2D.Float(0, 0, 1, 1));
  public static Shape heart    = createHeartShape();
  public static Shape star     = createStar(5);
  public static Shape cat      = createCatShape();
  public static Shape dog      = createDogShape();
  public static Shape fish     = createFishShape();

  // global objects

  JPanel    paintArea    = null;
  JPanel    colorArea    = null;
  JPanel    shapeArea    = null;
  Color     paintColor   = new Color(255, 0, 0, alpha);
  Shape     paintShape   = cat;
  Vector<Component> guiObjects = new Vector<Component>();
  JFrame    frame        = this;
  AudioClip lastPlayed   = null;
  boolean   soundEnabled = true;
  int       inputMode    = MOVEMODE;
  float     paintScale   = shapeScale;
  Calendar  startTime    = Calendar.getInstance();
  Color     backGround   = Color.WHITE;

  // help related objects

  Font  helpFont = new Font("Courier", Font.PLAIN, 40);
  Color helpColor = new Color(153, 153, 153);
  long  helpDelay = 5000;
  String[] helpText =
  {
    "ESC          exits program",
    "SPACE        toggles sound",
    "DOUBLE CLICK clears screen",
    "DOUBLE CLICK on a color sets background",
    "ENTER        cycles input mode",
    "",
    "input modes:",
    "  MOVE   all movments paint",
    "  STICKY click toggles paint mode",
    "  CLICK  click and drag to paint",
  };

  // some sounds

  SoundClip soundOn      = new SoundClip("soundon");
  SoundClip soundOff     = new SoundClip("soundoff");
  SoundClip moveMode     = new SoundClip("movemode");
  SoundClip clickMode    = new SoundClip("clickmode");
  SoundClip stickyMode   = new SoundClip("stickymode");
  SoundClip welcome      = new SoundClip("welcome");
  SoundClip goodBye      = new SoundClip("goodBye");

  // input mode sounds

  SoundClip[] modeSounds =
  {
    moveMode,
    stickyMode,
    clickMode,
  };

  // color pallet items

  PalletItem[] shapePallet =
  {
    new ShapePalletItem("Triangle", triangle),
    new ShapePalletItem("Square",   square),
    new ShapePalletItem("Pentagon", pentagon),
    new ShapePalletItem("Hexagon",  hexagon),
    new ShapePalletItem("Circle",   circle),
    new ShapePalletItem("Heart",    heart ),
    new ShapePalletItem("Star",     star),
    new ShapePalletItem("Doggy",    dog),
    new ShapePalletItem("Kitty",    cat),
    new ShapePalletItem("Fishy",    fish),
  };
  // color pallet items

  PalletItem[] colorPallet =
  {
    new ColorPalletItem("Black",  Color.BLACK),
    new ColorPalletItem("Gray",   new Color(128, 128, 128)),
    new ColorPalletItem("White",  Color.WHITE),
    new ColorPalletItem("Red",    Color.RED),
    new ColorPalletItem("Orange", new Color(255, 151, 0)),
    new ColorPalletItem("Yellow", Color.YELLOW),
    new ColorPalletItem("Green",  Color.GREEN),
    new ColorPalletItem("Blue",   Color.BLUE),
    new ColorPalletItem("Purple", new Color(0x75, 0x09, 0x91)),
    new ColorPalletItem("Pink",   new Color(255, 64, 196)),
  };

  //new ColorPalletItem("Light Red",    new Color(255, 80, 150)),
  //new ColorPalletItem("Turqoise",    new Color(30, 160, 188)),
  //new ColorPalletItem("Cadet Blue",    new Color(0, 96, 128)),
  //new ColorPalletItem("Sky Blue",    new Color(0, 64, 255)),

  ActionPalletItem[] actionPallet =
  {
    //new ActionPalletItem("Small", scale(circle, smallScale / mediumScale, smallScale / mediumScale))
    //{public void selected() {paintScale = smallScale; }},
    //new ActionPalletItem("Small", circle)
    //{public void selected() {paintScale = mediumScale;}},
    //new ActionPalletItem("Small", scale(circle, largeScale / mediumScale, largeScale / mediumScale))
    //{public void selected() {paintScale = largeScale; }},
  };

  // main enrty point for the program

  public static void main(String[] args)
  {
    try
    {
      new SarynPaint();
    }
    catch (Exception e)
    {
      System.out.println(e);
    }
  }

  // constructor

  public SarynPaint()
  {
    paintShape = createHeartShape();

    // construct the frame

    constructFrame(getContentPane());
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    // get the graphics device from the local graphic environment

    GraphicsDevice gv = GraphicsEnvironment.
      getLocalGraphicsEnvironment().getScreenDevices()[0];

    // if full screen is supported setup frame accoringly

    if (gv.isFullScreenSupported())
    {
      setUndecorated(true);
      setVisible(true);
      pack();
      gv.setFullScreenWindow(this);
    }
    // otherwise just make a big frame

    else
    {
      pack();
      setExtendedState(MAXIMIZED_BOTH);
      setVisible(true);
    }
    // set the cursor

    setPaintCursor();

    // play welcome

    welcome.play();
  }

  // place gui object into frame

  public void constructFrame(Container frame)
  {
    // set frame to box layout

    frame.setLayout(new BorderLayout());

    // add the color pallet

    frame.add(colorArea = new Pallet(colorPallet, BoxLayout.Y_AXIS), BorderLayout.WEST);

    // add the color pallet

    frame.add(shapeArea = new Pallet(shapePallet, BoxLayout.Y_AXIS), BorderLayout.EAST);

    // add action pallet

    //frame.add(new Pallet(actionPallet, BoxLayout.X_AXIS), BorderLayout.SOUTH);

    // paint area mouse listener

    MouseInputAdapter mia = new MouseInputAdapter()
      {
        // sticky paint mode enabled

        boolean sticky = false;

        // mouse dragged event

        public void mouseDragged(MouseEvent e)
        {
          drawShape(e);
        }
        // mouse moved event

        public void mouseMoved(MouseEvent e)
        {
          if (inputMode == MOVEMODE ||
            (inputMode == STICKYMODE && sticky))
            drawShape(e);
        }

        // mouse clicked event

        public void mouseClicked(MouseEvent e)
        {
          sticky = !sticky;

          if (inputMode != STICKYMODE || sticky)
            drawShape(e);

          if (e.getClickCount() > 1)
            clearScreen(e);
        }

        // draw shape in the pait area

        void drawShape(MouseEvent e)
        {
          Graphics2D g = (Graphics2D)paintArea.getGraphics();
          g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
          g.setColor(paintColor);
          paintShape(paintShape, g, e.getX(), e.getY());
        }

        // clear paint area

        void clearScreen(MouseEvent e)
        {
          paintArea.repaint();
        }
      };

    // setup paint area

    paintArea = new JPanel()
      {
        public void paint(Graphics g)
        {
          g.setColor(backGround);
          g.fillRect(0, 0, getWidth(), getHeight());
          if (Calendar.getInstance().getTimeInMillis() - 
            startTime.getTimeInMillis() < helpDelay)
          {
            drawHelpText((Graphics2D)g);
          }
        }
      };
    paintArea.addMouseMotionListener(mia);
    paintArea.addMouseListener(mia);
    frame.add(paintArea, BorderLayout.CENTER);

    // add a key listener

    addKeyListener(new KeyAdapter()
      {
        public void keyPressed(KeyEvent e)
        {
          // exists system

          if (e.getKeyChar() == KeyEvent.VK_ESCAPE)
          {
            try
            {
              goodBye.play();
              Thread.sleep(550);
              System.exit(0);
            }
            catch (Exception ex)
            {
              System.out.println(ex);
            }
          }
          // space toggles sound

          if (e.getKeyChar() == KeyEvent.VK_SPACE)
          {
            if (soundEnabled) soundOff.play();
            soundEnabled = !soundEnabled;
            if (soundEnabled) soundOn.play();
          }
          // slash toggles click mode

          if (e.getKeyChar() == KeyEvent.VK_ENTER)
          {
            inputMode++;
            inputMode %= MODECOUNT;
            modeSounds[inputMode].play();
          }
        }
      }
      );
  }

  // draw the help text onto the paint area

  void drawHelpText(Graphics2D g)
  {
    // set the font

    g.setFont(helpFont);

    // identify the bounds of the text

    FontMetrics fm = g.getFontMetrics();
    int width = 0;
    int height = 0;
    for (int i = 0; i < helpText.length; ++i)
    {
      Rectangle2D bounds = fm.getStringBounds(helpText[i], g);
      height += bounds.getHeight();
      if (width < bounds.getWidth())
        width = (int)bounds.getWidth();
    }

    // paint the text

    g.setColor(computeMatchingColor(backGround));
    int x = (int)((g.getClipBounds().getWidth () - width) / 2);
    int y = (int)((g.getClipBounds().getHeight() - height) / 2);
    for (int i = 0; i < helpText.length; ++i)
    {
      g.drawString(helpText[i], x, y);
      y += fm.getHeight();
    }
  }

  // compute matching color

  Color computeMatchingColor(Color color)
  {
    float brightness = Color.RGBtoHSB(
      color.getRed(),
      color.getGreen(),
      color.getBlue(),
      new float[3])[2];

    return brightness > 0.5
      ? color.darker().darker()
      : color.brighter().brighter().brighter()
      .brighter().brighter().brighter()
      .brighter().brighter().brighter();
  }

  // set cursor

  void setPaintCursor()
  {
    // create the cursor shape

    Shape shape = scale(paintShape, shapeScale, shapeScale);
    Rectangle2D bounds = shape.getBounds2D();
    shape = translate(shape, bounds.getWidth() / 2.0, bounds.getHeight() / 2.0);
    bounds = shape.getBounds2D();

    // create the cursor image

    BufferedImage image = new BufferedImage(
      (int)bounds.getWidth(),
      (int)bounds.getHeight() + 1,
      BufferedImage.TYPE_4BYTE_ABGR);

    // get the image graphics object

    Graphics2D g = image.createGraphics();
    g.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON);

    // paint the cursor shape into the image

    g.setColor(paintColor.darker());
    g.fill(shape);

    // create the custom cursor

    Toolkit tk = Toolkit.getDefaultToolkit();
    Cursor cursor = tk.createCustomCursor(
      image,
      new Point((int)(bounds.getWidth() / 2), (int)(bounds.getHeight() / 2)),
      "name");

    // set the cursor in all the right places

    paintArea.setCursor(cursor);
    for (int i = 0; i < guiObjects.size(); ++i)
      guiObjects.get(i).setCursor(cursor);
  }

  /** A generic pallet item base class. */

  public abstract class PalletItem extends JPanel
  {
    // audio clip associated with this item

    SoundClip sound = null;

    // name of pallet item

    String name = null;

    // construct pallet item

    PalletItem(String name)
    {
      // get the name

      this.name = name;

      // load the audo clip associated with this item

      sound = new SoundClip(name);

      // store a referance to this object in other places

      guiObjects.add(this);

      // add a mouse listener to identify when pallet item selected

      MouseInputAdapter mia = new MouseInputAdapter()
        {
          // mouse entered event

          public void mouseEntered(MouseEvent e)
          {
            if (inputMode != CLICKMODE) selectItem(e);
          }
          // mouse clicked event

          public void mouseClicked(MouseEvent e)
          {
            if (e.getClickCount() > 1)
              doubleClicked(e);

            if (inputMode == CLICKMODE) selectItem(e);
          }
          // select item

          public void selectItem(MouseEvent e)
          {
            sound.play();
            selected(e);
          }
        };
      addMouseListener(mia);
      addMouseMotionListener(mia);

      // set preferred size

      setPreferredSize(new Dimension(buttonWidth, buttonHeight));
    }

    // action to take when item selected

    abstract void selected(MouseEvent e);

    // action to take when item doubel clicked

    void doubleClicked(MouseEvent e)
    {
    }
  }

  /** A color pallet item. */

  public class ColorPalletItem extends PalletItem
  {
    Color color = null;

    ColorPalletItem(String name, Color color)
    {
      super(name);
      this.color = color;
    }

    // set background to this color

    void doubleClicked(MouseEvent e)
    {
      backGround = color;
      frame.repaint();
    }

    // set paint color when selected

    void selected(MouseEvent e)
    {
      paintColor = new Color(
        color.getRed(),
        color.getGreen(),
        color.getBlue(),
        alpha);
      setPaintCursor();
      shapeArea.repaint();
    }

    // when pallet item is painted

    public void paint(Graphics g)
    {
      g.setColor(color);
      g.fillRect(0, 0, getWidth(), getHeight());
    }
  }

  /** A shape pallet item. */

  public class ShapePalletItem extends PalletItem
  {
    Shape shape = null;

    ShapePalletItem(String name, Shape shape)
    {
      super(name);
      this.shape = shape;
    }

    // set paint color when selected

    void selected(MouseEvent e)
    {
      paintShape = shape;
      setPaintCursor();
    }

    // when pallet item is painted

    public void paint(Graphics graphics)
    {
      Graphics2D g = (Graphics2D)graphics;
      g.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(backGround);
      Rectangle bounds = this.getBounds(new Rectangle());
      g.fillRect(0, 0, getWidth(), getHeight());
      Color shapeColor = new Color(
        paintColor.getRed(),
        paintColor.getGreen(),
        paintColor.getBlue());
      if (shapeColor.equals(backGround))
        shapeColor = computeMatchingColor(shapeColor);
      g.setColor(shapeColor);
      paintShape(shape, g, getWidth() / 2.0, getHeight() / 2.0);
    }
  }

  /** An action pallet item (not currently working). */

  public class ActionPalletItem extends ShapePalletItem
  {
    ActionPalletItem(String name, Shape icon)
    {
      super(name, icon);
    }
  }

  /** A pallet of color or shape tools. */

  public class Pallet extends JPanel
  {
    // construct a pallet

    Pallet(PalletItem[] palletItems, int axis)
    {
      setLayout(new BoxLayout(this, axis));
      //add(Box.createVerticalGlue());
      for (int i = 0; i < palletItems.length; ++i)
        add(palletItems[i]);
      //add(Box.createVerticalGlue());
    }

    // paint a pallet

    public void paint(Graphics graphics)
    {
      Graphics2D g = (Graphics2D)graphics;
      g.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
      //g.setColor(backGround); //new Color(255, 0, 0, 128));
      //g.fillRect(0, 0, getWidth(), getHeight());
      setBackground(backGround);
      super.paint(g);
    }
  }

  /** A sound clip which can be loaded and played. */

  public class SoundClip
  {
    AudioClip sound       = null;
    String    name        = null;
    boolean   loadAttempt = false;

    // construct a sound clip, with no preload

    public SoundClip(String name)
    {
      this(name, false);
    }

    // construct a sound clip with optional preload

    public SoundClip(String name, boolean preload)
    {
      this.name = name;
      if (preload) load();
    }

    // play a sound

    public void play()
    {
      // if sound disabled, return

      if (!soundEnabled)
        return;

      // if no attempt has been made to load sound, to do so

      if (loadAttempt == false)
        load();

      // if the sound was loaded play it

      if (sound != null)
        sound.play();
    }

    // load sound clip

    public void load()
    {
      try
      {
        // a load attemt has occered

        loadAttempt = true;

        // establish the url to the file

        URL url = this.getClass().getResource(
          "/sounds/" + name.toLowerCase() + ".wav");

        // if the file exists load the audo clip

        if (url != null)
          sound = Applet.newAudioClip(url);
      }
      catch (Exception e)
      {
        System.out.println("Exception: " + e);
      }
    }
  }

  // create heart shape

  public static Shape createHeartShape()
  {
    GeneralPath gp = new GeneralPath();
    gp.append(translate(circle, 0.5, 0), false);
    gp.append(translate(circle, 0, 0.5), false);
    gp.append(square, false);
    return normalize(rotate(gp, 225));
  }

  // create cat shape

  public static Shape createCatShape()
  {
    Area cat = new Area(circle);
    Area wisker = new Area(new Rectangle2D.Double(0, -.01, .3, .02));

    // create left wiskes

    Area leftWiskers = new Area();
    leftWiskers.add(rotate(wisker, -20));
    leftWiskers.add(rotate(wisker,  20));
    leftWiskers.add(rotate(wisker,  20));

    // create right wiskers

    Area rightWiskers = new Area();
    rightWiskers.add(rotate(wisker, 180));
    rightWiskers.add(rotate(wisker, -20));
    rightWiskers.add(rotate(wisker, -20));

    // add the ears

    Area ear = new Area(translate(scale(triangle, .5, .5), 0.0, -0.6));
    translate(ear, .07, 0);
    cat.add(ear);
    rotate(cat, 60);
    translate(ear, -.14, 0);
    cat.add(ear);
    rotate(cat, -30);

    // add the eyes

    Area eye = new Area(scale(circle, 0.18, 0.18));
    eye.subtract(new Area(scale(circle, .06, .12)));
    translate(eye, -.15, -.1);
    cat.subtract(eye);
    translate(eye, .3, 0);
    cat.subtract(eye);

    // add the wiskers

    cat.subtract(translate(leftWiskers,   .08, .14));
    cat.subtract(translate(rightWiskers, -.08, .14));

    // add nose

    Area nose = new Area(createRegularPoly(3));
    rotate(nose, 180);
    scale(nose, .15, .15);
    translate(nose, 0, .1);
    cat.subtract(nose);

    // flatten the cat

    scale(cat, 1.0, 0.85);

    // return normalized shape

    return normalize(cat);
  }

  // create dog shape

  public static Shape createDogShape()
  {
    Area dog = new Area(circle);

    // add the ears

    Area ear = new Area(scale(circle, .4, .7));
    rotate(ear, 20);
    translate(ear, -.5, -.2);
    dog.subtract(ear);
    scale(ear, -1, 1);
    dog.subtract(ear);
    scale(ear, -1, 1);
    translate(ear, -.05, 0);
    dog.add(ear);
    scale(ear, -1, 1);
    dog.add(ear);
    scale(ear, -1, 1);

    // add the eyes

    Area eye = new Area(scale(circle, 0.18, 0.18));
    eye.subtract(new Area(scale(circle, .12, .12)));
    translate(eye, -.15, -.1);
    dog.subtract(eye);
    translate(eye, .3, 0);
    dog.subtract(eye);

    // add nose

    Area snout = new Area(circle);
    scale(snout, .30, .30);
    translate(snout, 0, .2);
    dog.subtract(snout);

    // add nose

    Area nose = new Area(createRegularPoly(3));
    rotate(nose, 180);
    scale(nose, .20, .20);
    translate(nose, 0, .2);
    dog.add(nose);

    // stretch the dog

    scale(dog, 0.90, 1.0);

    // return normalized shape

    return normalize(dog);
  }

  // create dog shape

  public static Shape createFishShape()
  {
    Area fish = new Area();
    Area body = new Area(new Arc2D.Double(0.0, 0, 1.0, 1.0, 30, 120, Arc2D.CHORD));
    Rectangle2D bounds = body.getBounds2D();
    translate(body,
      -(bounds.getX() + bounds.getWidth()  / 2),
      -bounds.getHeight());
    fish.add(body);
    scale(body, 1, -1);
    fish.add(body);

    // add the eye

    Area eye = new Area(scale(circle, .13, .13));
    eye.subtract(new Area(scale(circle, .08, .08)));
    translate(eye, -.15, -.08);
    fish.subtract(eye);

    // add tail

    Area tail = new Area(normalize(rotate(triangle, 30)));
    scale(tail, .50, .50);
    translate(tail, .4, 0);
    fish.add(tail);

    // return normalized shape

    return normalize(fish);
  }

  // create regular polygon

  public static Shape createRegularPoly(int edges)
  {
    double radius = 1000;
    double theta = 0.75 * (2 * Math.PI);
    double dTheta = (2 * Math.PI) / edges;
    Polygon p = new Polygon();

    // add a point for each edge

    for (int edge = 0; edge < edges; ++edge)
    {
      p.addPoint(
        (int)(Math.cos(theta) * radius),
        (int)(Math.sin(theta) * radius));
      theta += dTheta;
    }
    // return the normalized poly

    return normalize(p);
  }

  // create star

  public static Shape createStar(int points)
  {
    double radius = 1000;
    double theta = 0.75 * (2 * Math.PI);
    double dTheta = (4 * Math.PI) / points;
    Polygon p = new Polygon();

    // add a point for each edge

    for (int point = 0; point < points; ++point)
    {
      p.addPoint(
        (int)(Math.cos(theta) * radius),
        (int)(Math.sin(theta) * radius));
      theta += dTheta;
    }
    // convert to a general path to fill the shape

    GeneralPath gp = new GeneralPath(GeneralPath.WIND_NON_ZERO);
    gp.append(p, true);

    // return the normalized star

    return normalize(gp);
  }

  // normalize shape (centered at origin, length & with <= 1.0)

  public static Shape normalize(Shape shape)
  {
    // center the shape on the origin

    Rectangle2D bounds = shape.getBounds2D();
    shape = translate(shape,
      -(bounds.getX() + bounds.getWidth() / 2),
      -(bounds.getY() + bounds.getHeight() / 2));

    // normalize size

    bounds = shape.getBounds2D();
    double scale = bounds.getWidth() > bounds.getHeight()
      ? 1.0 / bounds.getWidth()
      : 1.0 / bounds.getHeight();
    return scale(shape, scale, scale);
  }

  // rotate a shape

  public static Shape rotate(Shape shape, double degrees)
  {
    return AffineTransform.getRotateInstance(degrees / 180 * Math.PI)
      .createTransformedShape(shape);
  }

  // translate a shape

  public static Shape translate(Shape shape, double x, double y)
  {
    return AffineTransform.getTranslateInstance(x, y).createTransformedShape(shape);
  }

  // scale a shape

  public static Shape scale(Shape shape, double x, double y)
  {
    return AffineTransform.getScaleInstance(x, y).createTransformedShape(shape);
  }

  // rotate an area

  public static Area rotate(Area area, double degrees)
  {
    area.transform(AffineTransform.getRotateInstance(degrees / 180 * Math.PI));
    return area;
  }

  // translate an area

  public static Area translate(Area area, double x, double y)
  {
    area.transform(AffineTransform.getTranslateInstance(x, y));
    return area;
  }

  // scale an area

  public static Area scale(Area area, double x, double y)
  {
    area.transform(AffineTransform.getScaleInstance(x, y));
    return area;
  }

  // paint a shape at a given location

  public Shape paintShape(Shape shape, Graphics2D g, double x, double y)
  {
    shape = translate(scale(shape, paintScale, paintScale), x, y);
    g.fill(shape);
    return shape;
  }
}
