package PNGto4BPP;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.DataBufferByte;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import java.io.FileInputStream; 

// class
public class PNGto4BPP {
	// to spit out errors
	public PNGto4BPP() {super();}
	static final PNGto4BPP controller = new PNGto4BPP();
	// accepted extensions
	final static String[] IMAGEEXTS = { "png" };
	final static String[] PALETTEEXTS = { "gpl", "pal", "txt" };
	final static String[] EXPORTEXTS = { "spr" };
	final static String[] LOGEXTS = { "txt" };

	//These fields are utilized by functions
	static final JTextField imageName = new JTextField("");
	static final JTextField palName = new JTextField("");
	static final JTextField fileName = new JTextField("");
// palette reading methods
	static String[] palChoices = {
				"Read ASCII (" + join(PALETTEEXTS,", ") +")",
				"Binary (YY-CHR .PAL)",
				"Extract from last block of PNG"
				};
	static final JComboBox<String> palOptions = new JComboBox<String>(palChoices);
	static final JFrame frame = new JFrame("PNG to SNES 4BPP");

	static StringWriter debugLogging;
	static PrintWriter debugWriter;

	// Summary
	// Command Line Usage:
	// imgSrc: Full Path for Image
	// palMethod: palFileMethod [0:ASCII(.GPL|.PAL), 1:Binary(YY .PAL), 2:Extract from Last Block of PNG]
	// palSrc:(Used if Method 0 or 1 selected): Full Path for Pal File.
	// sprTarget(optional): Name of Sprite that will be created. Will default to name of imgSrc with new extension. 
	// romTarget(optional): Path of Rom to patch.

	// main and stuff
	public static void main(String[] args) {
		//try to set Nimbus
		try {
			NimbusLookAndFeel lookAndFeel = new NimbusLookAndFeel();
			UIManager.setLookAndFeel(lookAndFeel);
		} catch (UnsupportedLookAndFeelException e) {
			// try to set System default
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (UnsupportedLookAndFeelException
					| ClassNotFoundException
					| InstantiationException
					| IllegalAccessException e2) {
					// do nothing
			} //end System
		} // end Nimbus

		// window building
		final JFrame frame = new JFrame("PNG to SNES 4BPP");
		final JFrame debugFrame = new JFrame("Debug");
		final JFrame aboutFrame = new JFrame("About");
		final Dimension d = new Dimension(600,382);
		final Dimension d2 = new Dimension(600,600);

		final TextArea debugLog = new TextArea("Debug log:",0,0,TextArea.SCROLLBARS_VERTICAL_ONLY);
		debugLog.setEditable(false);
		// buttons
		final JButton imageBtn = new JButton("Load PNG");
		final JButton palBtn = new JButton("Load Palette");
		final JButton fileNameBtn = new JButton("Set Export Filename");
		final JButton runBtn = new JButton("Convert!");
		final JButton clrLog = new JButton("Clear");
		final JButton expLog = new JButton("Export");
		// acknowledgements
		final JMenuItem peeps = new JMenuItem("About");
		final TextArea peepsList = new TextArea("", 0,0,TextArea.SCROLLBARS_VERTICAL_ONLY);
		peepsList.setEditable(false);
		peepsList.append("Written by fatmanspanda"); // hey, that's me
		peepsList.append("\n\nSpecial thanks:\nMikeTrethewey"); // forced me to do this and falls in every category
		peepsList.append("\n\nCode contribution:\n");
		peepsList.append(join(new String[]{
				"Zarby89", // a lot of conversion help
				"Glan", // various optimizations and bitwise help
				"CGG Zayik" // command line functions
				}, ", "));
		peepsList.append("\n\nTesting and feedback:\n");
		peepsList.append(join(new String[]{
				"CGG Zayik", // test sprite contributor
				"RyuTech", // test sprite contributor
				"Damon" // test sprite contributor
				}, ", "));
		peepsList.append("\n\nResources and development:\n");
		peepsList.append(join(new String[]{
				"Veetorp", // provided most valuable documentation
				"Zarby89", // various documentation and answers
				"Damon", // Paint.NET palettes
				"Sosuke3" // various snes code answers
				}, ", "));
		aboutFrame.add(peepsList);
		// debug text
		final JPanel debugWrapper = new JPanel(new BorderLayout());
		debugWrapper.add(clrLog,BorderLayout.WEST);
		debugWrapper.add(expLog,BorderLayout.EAST);
		debugFrame.add(debugLog);
		debugFrame.add(debugWrapper,BorderLayout.SOUTH);
		debugLogging = new StringWriter();
		debugWriter = new PrintWriter(debugLogging);
		debugLogging.write("Debug log:\n");
		// menu 
		final JMenuBar menu = new JMenuBar();
		final JMenuItem debug = new JMenuItem("Debug");
		menu.add(debug);
		menu.add(peeps);
		frame.setJMenuBar(menu);
		// file explorer
		final JFileChooser explorer = new JFileChooser();
		// set filters
		FileNameExtensionFilter imgFilter =
				new FileNameExtensionFilter("PNG files", IMAGEEXTS);
		FileNameExtensionFilter palFilter =
				new FileNameExtensionFilter("Palette files (" + join(PALETTEEXTS,", ") +")",
						PALETTEEXTS);
		FileNameExtensionFilter sprFilter =
				new FileNameExtensionFilter("ALttP Sprite files", EXPORTEXTS);
		FileNameExtensionFilter logFilter =
				new FileNameExtensionFilter("text files", LOGEXTS);

		explorer.setAcceptAllFileFilterUsed(false);

		final JPanel frame2 = new JPanel(new BorderLayout());
		final JPanel imgPalWrapper = new JPanel(new BorderLayout());
		final JPanel imgNWrapper = new JPanel(new BorderLayout());
		final JPanel palNWrapper = new JPanel(new BorderLayout());
		final JPanel palBtnWrapper = new JPanel(new BorderLayout());
		final JPanel fileNWrapper = new JPanel(new BorderLayout());
		final JPanel allWrapper = new JPanel(new BorderLayout());
		// add image button and field
		imgNWrapper.add(imageName,BorderLayout.CENTER);
		imgNWrapper.add(imageBtn,BorderLayout.EAST);
		imgPalWrapper.add(imgNWrapper,BorderLayout.NORTH);
		// add palette button and field
		palNWrapper.add(palName,BorderLayout.CENTER);
		palBtnWrapper.add(palBtn,BorderLayout.WEST);
		palBtnWrapper.add(palOptions,BorderLayout.EAST);
		palNWrapper.add(palBtnWrapper,BorderLayout.EAST);
		imgPalWrapper.add(palNWrapper,BorderLayout.SOUTH);
		// add new file button and field
		fileNWrapper.add(fileName,BorderLayout.CENTER);
		fileNWrapper.add(fileNameBtn,BorderLayout.EAST);
		// add run button
		fileNWrapper.add(runBtn,BorderLayout.SOUTH);
		allWrapper.add(imgPalWrapper,BorderLayout.NORTH);
		allWrapper.add(fileNWrapper,BorderLayout.SOUTH);
		// add wrappers
		frame2.add(allWrapper,BorderLayout.NORTH);
		frame.add(frame2);
		frame.setSize(d);
		debugFrame.setSize(d2);
		aboutFrame.setSize(d2);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.setLocation(200,200);
		frame.setVisible(true);
		// can't clear text due to wonky code
		// have to set a blank file instead
		final File EEE = new File("");

		// about
		peeps.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				aboutFrame.setVisible(true);
			}});
		// debug
		debug.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				debugLog.setText(debugLogging.toString());
				debugFrame.setVisible(true);
			}});
		// debug clear
		clrLog.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				debugLogging.flush();
				debugLogging.getBuffer().setLength(0);
				debugLogging.write("Debug log:\n");
				debugLog.setText(debugLogging.toString());
			}});
		// export log to a text file
		expLog.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				explorer.setSelectedFile(new File("error log (" + System.currentTimeMillis() + ").txt"));
				explorer.setFileFilter(logFilter);
				int option = explorer.showSaveDialog(expLog);
				String n = "";
				try {
					n = explorer.getSelectedFile().getPath();
				} catch (NullPointerException e) {
					// do nothing
				} finally {
					if (!testFileType(n,LOGEXTS)) {
						JOptionPane.showMessageDialog(frame,
								"Debug logs must be of the following extensions:\n" + join(LOGEXTS,", "),
								"Oops",
								JOptionPane.WARNING_MESSAGE);
						return;
					}
				}
				explorer.removeChoosableFileFilter(logFilter);
				if (option == JFileChooser.CANCEL_OPTION)
					return;
				PrintWriter logBugs;
				try {
					logBugs = new PrintWriter(n);
				} catch (FileNotFoundException e) {
					JOptionPane.showMessageDialog(frame,
							"There was a problem writing to the log!",
							"WOW",
							JOptionPane.WARNING_MESSAGE);
					e.printStackTrace(debugWriter);
					return;
				}
				logBugs.write(debugLogging.toString());
				logBugs.close();
				JOptionPane.showMessageDialog(frame,
						"Error log written to:\n" + n,
						"YAY",
						JOptionPane.PLAIN_MESSAGE);
			}});
		// image button
		imageBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				explorer.setSelectedFile(EEE);
				explorer.setFileFilter(imgFilter);
				explorer.showOpenDialog(imageBtn);
				String n = "";
				try {
					n = explorer.getSelectedFile().getPath();
				} catch (NullPointerException e) {
					// do nothing
				} finally {
					if (testFileType(n,IMAGEEXTS))
						imageName.setText(n);
				}
				explorer.removeChoosableFileFilter(imgFilter);
			}});

		// palette button
		palBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				explorer.setSelectedFile(EEE);
				explorer.setFileFilter(palFilter);
				explorer.showOpenDialog(palBtn);
				String n = "";
				try {
					n = explorer.getSelectedFile().getPath();
				} catch (NullPointerException e) {
					// do nothing
				} finally {
					if (testFileType(n,PALETTEEXTS))
						palName.setText(n);
				}
				explorer.removeChoosableFileFilter(palFilter);
			}});

		// file name button
		fileNameBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				explorer.setSelectedFile(EEE);
				explorer.setFileFilter(sprFilter);
				explorer.showOpenDialog(fileNameBtn);
				String n = "";
				try {
					n = explorer.getSelectedFile().getPath();
				} catch (NullPointerException e) {
					// do nothing
				} finally {
					if (testFileType(n,EXPORTEXTS))
						fileName.setText(n);
				}
				explorer.removeChoosableFileFilter(sprFilter);
			}});

		// run button
		runBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				ConvertPngToSprite(false);	
			}});

		//If arguments are greater than 1, then we have the neccesary arguemnts to do command line processing.
		if(args.length > 1) {
			//If we encountered no errors processing the arguments, then convert and end program.
			if(ProcessArgs(args)) {
				System.exit(0);
			}
			//Leave program open with the fields that succeeded in being set.
			else {
				System.out.println("Failed to Process Arguments.");
			}
		}

	}

// Summary
	// ProcessArgs checks if the arguments are valid, and if so, sets the TextFields and ComboBox with the values from the passed arguments.
	// Returns True if arguments were processed successfuly. False if not. 
	public static boolean ProcessArgs(String[] args) {		
		if(args.length < 2 || args.length > 5) {
			return false;
		}
		String imgSrc= "";
		String palSrc = "";
		String sprTarget = "";
		String romTarget = "";
		int palOption = -1;
		boolean argumentErrorsFound = false;

		for(int i = 0; i < args.length; i++) {
			//Tokenize arg
			String[] tokens = args[i].split("=");
			//System.out.println(tokens[0]);

			// imgSrc: Full Path for Image
			// palOption: palFileOption [0:ASCII(.GPL|.PAL), 1:Binary(YY .PAL), 2:Extract from Last Block of PNG]
			// palSrc:(Used if Option 0 or 1 selected) Full Path for Pal File.
			// sprTarget: Name of Sprite that will be created.
			if(tokens.length == 2) {
				switch(tokens[0]) {
					case "imgSrc":
						imgSrc = tokens[1];
						imageName.setText(imgSrc);
						break;
					case "palOption":
						if(IsInteger(tokens[i])) {
							palOption = Integer.parseInt(tokens[1]);

							if(palOption >= 0 && palOption < palOptions.getItemCount()) {
								palOptions.setSelectedIndex(palOption);
							}
							else {
								System.out.println("The palOption: " + palOption + " is out of range.");
								argumentErrorsFound = true;
							}
						}
						else {
							System.out.println("The argument: " + tokens[1] + " is not a valid integer to specify the palette Option. 0: ASCII, 1:Binary, 2:Extract from Last Block of PNG");
							argumentErrorsFound = true;
						}
						break;
					case "palSrc":
						palSrc = tokens[1];
						palName.setText(palSrc);
						break;
					case "sprTarget":
						sprTarget = tokens[1];
						fileName.setText(sprTarget);
						break;
					case "romTarget":
						romTarget = tokens[1];						
						break;
				}
			}
			else {
				System.out.println("The argument: " + args[i] + " is invalid.");
				argumentErrorsFound = true;
			}			
		} //End of For Loops				

		if(argumentErrorsFound)
			return false;

		// Ensure imgSrc exists
		if(imgSrc == "") {
			System.out.println("No Source Image was specified or was not specified correctly.");
			argumentErrorsFound = true;
		}

		// enters here if palMethod is between 1-3
		if(palSrc == "" && (palOption == 0 || palOption == 1)) {
			System.out.println("No palette Source was specified despite using a palette method that requires it.");
			argumentErrorsFound = true;
		}

		// If sprite target name is not set, use the img source name with .spr extension.
		if(sprTarget == "") {
			sprTarget = ChangeExtension(imgSrc, "spr");
			fileName.setText(sprTarget);
		}

		// If all arguments check out, lets finish everything we want to do.
		if(!argumentErrorsFound) {
			// Returns true if successful
			if(ConvertPngToSprite(true)) {

				if(romTarget != "") {
					try {
						// Push change to ROM
						UpdateRom(sprTarget, romTarget);
					} catch (IOException e) {
						System.out.println("ERROR: " + e);
						return false;
					}
				}
			}
		}
		return !argumentErrorsFound;
	}

	public static void UpdateRom(String sprTarget, String romTarget) throws IOException, FileNotFoundException {
		byte[] rom_patch;
		byte[] sprite_data = new byte[0x7078];

		// filestream open .spr file
		FileInputStream fsInput = new FileInputStream(sprTarget);
		fsInput.read(sprite_data);
		fsInput.close();

		// Acquire rom data
		fsInput = new FileInputStream(romTarget);
		rom_patch = new byte[(int)fsInput.getChannel().size()];
		fsInput.read(rom_patch);
		fsInput.getChannel().position(0);
		fsInput.close();

		// filestream save .spr file to rom
		FileOutputStream fsOut = new FileOutputStream(romTarget);

		for(int i = 0;i<0x7000;i++) {
			rom_patch[0x80000 + i] = sprite_data[i];
		}
		for (int i = 0; i < 0x78; i++) {
			rom_patch[0x0DD308 + i] = sprite_data[i+0x7000];
		}
		fsOut.write(rom_patch, 0, rom_patch.length);

		fsOut.close();
	}

	public static boolean IsInteger(String string) {
		try {
			Integer.valueOf(string);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static String ChangeExtension(String file, String extension) {
		String filename = file;

		if (filename.contains(".")) {
			filename = filename.substring(0, filename.lastIndexOf('.'));
		}
		filename += "." + extension;

		return filename;
	}

	public static boolean ConvertPngToSprite(boolean ignoreSuccessMessage) {
		byte[] rando = new byte[16];
		for (int i = 0; i < rando.length; i++) // initialize rando with -1 to prevent static in the trans areas
			rando[i] = -1;
		BufferedImage img;
		BufferedImage imgRead;
		byte[] pixels;
		String imgName = imageName.getText();
		String paletteName = palName.getText();
		File imageFile = new File(imgName);
		BufferedReader br;
		int[] palette = null;
		byte[] palData = null;
		byte[][][] eightbyeight;
		int palChoice = palOptions.getSelectedIndex(); // see which palette method we're using
		boolean extensionERR = false; // let the program spit out all extension errors at once

		// test image type
		if (!testFileType(imgName,IMAGEEXTS)) {
			JOptionPane.showMessageDialog(frame,
					"Images must be of the following extensions:\n" + join(IMAGEEXTS,", "),
					"Oops",
					JOptionPane.WARNING_MESSAGE);
			extensionERR = true;
		}

		// test palette type
		if (!testFileType(paletteName,PALETTEEXTS) && (palChoice != 2)) {
			if(paletteName.length() == 0) {
				JOptionPane.showMessageDialog(frame,
						"No Palette source was specified despite using a palette method that requires it",
						"Oops",
						JOptionPane.WARNING_MESSAGE);
				extensionERR = true;
			} else {
				JOptionPane.showMessageDialog(frame,
						"Palettes must be of the following extensions:\n" + join(PALETTEEXTS,", "),
						"Oops",
						JOptionPane.WARNING_MESSAGE);
				extensionERR = true;
			}
		}

		// save location
		String loc = fileName.getText();
		boolean bamboozled = false;
		boolean[] bamboozarino = new boolean[16];
		if (loc.toLowerCase().matches("bamboozle:\\s*[0-9a-f]+")) {
			bamboozled = true;
			loc = loc.replace("bamboozle:","");
			String HEX = "0123456789ABCDEF";
			for (int i = 0; i < HEX.length(); i++) {
				char a = HEX.charAt(i);
				if (loc.indexOf(a) != -1) {
					bamboozarino[i] = true;
				}
			}

			for (int i = 0; i < bamboozarino.length; i++) {
				if (bamboozarino[i]) {
					rando[i] = (byte) i;
				}
			}
			loc = "";
		}

		// default name
		if (loc.equals("")) {
			loc = imgName;
			try {
				loc = loc.substring(0,loc.lastIndexOf("."));
			} catch(StringIndexOutOfBoundsException e) {
				loc = "oops";
			} finally {
				// still add extension here so that the user isn't fooled into thinking they need this field
				loc += " (" + (bamboozled ? "bamboozled" : "exported") + ").spr";
			}
		}

		// only allow sprite files
		if (!testFileType(loc,EXPORTEXTS)) {
			JOptionPane.showMessageDialog(frame,
					"Export location must be of the following extensions:\n" + join(EXPORTEXTS,", "),
					"Oops",
					JOptionPane.WARNING_MESSAGE);
			extensionERR = true;
		}

		// break if any extension related errors
		if (extensionERR) {
			return false;
		}

		// image file
		try {
			imgRead = ImageIO.read(imageFile);
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(frame,
					"Image file not found",
					"Oops",
					JOptionPane.WARNING_MESSAGE);
			e.printStackTrace(debugWriter);
			return false;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(frame,
					"Error reading image",
					"Oops",
					JOptionPane.WARNING_MESSAGE);
			e.printStackTrace(debugWriter);
			return false;
		}

		// convert to RGB colorspace
		img = convertToABGR(imgRead);

		// image raster
		try {
			pixels = getImageRaster(img);
		} catch (BadDimensionsException e) {
			JOptionPane.showMessageDialog(frame,
					"Image dimensions must be 128x448",
					"Oops",
					JOptionPane.WARNING_MESSAGE);
			e.printStackTrace(debugWriter);
			return false;
		}

		// round image raster
		pixels = roundRaster(pixels);

		// explicit ASCII palette
		if (palChoice == 0) {
			// get palette file
			try {
				br = getPaletteFile(paletteName);
			} catch (FileNotFoundException e) {
				JOptionPane.showMessageDialog(frame,
						"Palette file not found",
						"Oops",
						JOptionPane.WARNING_MESSAGE);
				e.printStackTrace(debugWriter);
				return false;
			}
			// palette parsing
			try {
				// test file type to determine format
				if (testFileType(paletteName, "txt"))
					palette = getPaletteColorsFromPaintNET(br);
				else
					palette = getPaletteColorsFromFile(br);
				palette = roundPalette(palette);
				palData = palDataFromArray(palette);
			} catch (NumberFormatException|IOException e) {
				JOptionPane.showMessageDialog(frame,
						"Error reading palette",
						"Oops",
						JOptionPane.WARNING_MESSAGE);
				e.printStackTrace(debugWriter);
				return false;
			} catch (ShortPaletteException e) {
				JOptionPane.showMessageDialog(frame,
						"Unable to find 16 colors",
						"Oops",
						JOptionPane.WARNING_MESSAGE);
				e.printStackTrace(debugWriter);
				return false;
			}
		}

		// binary (YY-CHR) pal
		// TODO: this
		if (palChoice == 1) {
			// FIXME Binary Palette reading
			JOptionPane.showMessageDialog(frame,
					"Binary Palette reading (such as for YY-CHR) is not available at this time",
					"Oops",
					JOptionPane.WARNING_MESSAGE);
			extensionERR = true;
			return false;

//			if (!testFileType(paletteName, "pal")) {
//				JOptionPane.showMessageDialog(frame,
//						"Binary palette reading must by a .PAL file",
//						"Oops",
//						JOptionPane.WARNING_MESSAGE);
//				return false;
//			}
//			try {
//				byte[] palX = readFile(paletteName);
//				palette = palFromBinary(palX);
//				palData = palDataFromArray(palette);
//			} catch(Exception e) {
//				return false;
//			}
		}

		// extract from last block
		if (palChoice == 2) {
			palette = palExtract(pixels);
			palData = palDataFromArray(palette);
		}

		// make the file
		try {
			new File(loc);
		} catch (NullPointerException e) {
			JOptionPane.showMessageDialog(frame,
					"Invalid file name",
					"Oops",
					JOptionPane.WARNING_MESSAGE);
			e.printStackTrace(debugWriter);
		}

		// split bytes into blocks
		eightbyeight = get8x8(pixels, palette);

		byte[] SNESdata = exportPNG(eightbyeight, palData, rando);

		// write data to SPR file
		try {
			writeSPR(SNESdata, loc);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(frame,
					"Error writing sprite",
					"Oops",
					JOptionPane.WARNING_MESSAGE);
			e.printStackTrace(debugWriter);
			return false;
		}

		if(!ignoreSuccessMessage) {
			// success
			JOptionPane.showMessageDialog(frame,
				"Sprite file successfully written to " + (new File(loc).getName()),
				"YAY",
				JOptionPane.PLAIN_MESSAGE);
		}		
		return true;
	}

	/**
	 * gives file extension name from a string
	 * @param s - test case
	 * @return extension type
	 */
	public static String getFileType(String s) {
		String ret = s.substring(s.lastIndexOf(".") + 1);
		return ret;
	}

	/**
	 * Test a file against multiple extensions.
	 * The way <b>getFileType</b> works should allow
	 * both full paths and lone file types to work.
	 * 
	 * @param s - file name or extension
	 * @param type - list of all extensions to test against
	 * @return <tt>true</tt> if any extension is matched
	 */
	public static boolean testFileType(String s, String[] type) {
		boolean ret = false;
		String filesType = getFileType(s);
		for (String t : type) {
			if (filesType.equalsIgnoreCase(t)) {
				ret = true;
				break;
			}
		}
		return ret;
	}

	/**
	 * Test a file against a single extension.
	 * 
	 * @param s - file name or extension
	 * @param type - extension
	 * @return <tt>true</tt> if extension is matched
	 */
	public static boolean testFileType(String s, String type) {
		return testFileType(s, new String[] { type });
	}

	/**
	 * Join array of strings together with a delimiter.
	 * @param s - array of strings
	 * @param c - delimiter
	 * @return A single <tt>String</tt>.
	 */
	public static String join(String[] s, String c) {
		String ret = "";
		for (int i = 0; i < s.length; i++) {
			ret += s[i];
			if (i != s.length-1) {
				ret += c;
			}
		}
		return ret;
	}

	/**
	 * Converts to ABGR colorspace
	 * @param img - image to convert
	 * @return New <tt>BufferredImage</tt> in the correct colorspace
	 */

	public static BufferedImage convertToABGR(BufferedImage img) {
		BufferedImage ret = null;
		ret = new BufferedImage(img.getWidth(),img.getHeight(),BufferedImage.TYPE_4BYTE_ABGR);
		ColorConvertOp rgb = new ColorConvertOp(null);
		rgb.filter(img,ret);
		return ret;
	}

	/**
	 * Get the full image raster
	 * @param img - image to read
	 * @throws BadDimensionsException if the image is not 128 pixels wide and 448 pixels tall
	 */
	public static byte[] getImageRaster(BufferedImage img) throws BadDimensionsException {
		int w = img.getWidth();
		int h = img.getHeight();
		if (w != 128 || h != 448) {
			throw controller.new BadDimensionsException("Invalid dimensions of {" + w + "," + h + "}");
		}
		byte[] pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
		return pixels;
	}

	/**
	 * Finds the palette file (as a .gpl or .pal) from <tt>palPath<tt>
	 * @param palPath - full file path of the palette
	 * @throws FileNotFoundException
	 */
	public static BufferedReader getPaletteFile(String palPath)	throws FileNotFoundException {
		FileReader pal = new FileReader(palPath);
		BufferedReader ret = new BufferedReader(pal);
		return ret;
	}

	public static byte[] readFile(String path) throws IOException {
		File file = new File(path);
		byte[] ret = new byte[(int) file.length()];
		FileInputStream s;
		try {
			s = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw e;
		}
		try {
			s.read(ret);
			s.close();
		} catch (IOException e) {
			throw e;
		}

		return ret;
	}
	/**
	 * Reads a GIMP (<tt>.gpl</tt>) or Graphics Gale (<tt>.pal</tt>) palette file for colors.
	 * <br><br>
	 * This function first finds as many colors as it can from the palette.
	 * Once the palette is fully read, the number of colors recognized is
	 * rounded down to the nearest multiple of 16.
	 * Each multiple of 16 represents one of Link's mail palettes
	 * (green, blue, red, bunny).
	 * If fewer than 4 palettes are found, any empty palette is copied from green mail.
	 * 
	 * @param pal - Palette to read
	 * @return <b>int[]</b> of 64 colors as integers (RRRGGGBBB)
	 * @throws ShortPaletteException Halts the process if enough colors are not found.
	 */
	public static int[] getPaletteColorsFromFile(BufferedReader pal)
			throws NumberFormatException, IOException, ShortPaletteException {
		int[] palret = new int[64];
		String line;

		// read palette
		int pali = 0;
		while ( (line = pal.readLine()) != null) {
			// look with 3 numbers
			String[] line2 = (line.trim()).split("\\D+");
			int colori = 0;
			int[] colorArray = new int[3];
			if (line2.length >= 3) {
				for (String s : line2) {
					int curCol = -1;
					try {
						curCol = Integer.parseInt(s);
					} catch(NumberFormatException e) {
						// nothing
					} finally {
						colorArray[colori] = curCol;
						colori++;
					}
					if (colori == 3) {
						break;
					}
				}
				// read RGB bytes as ints
				int r = colorArray[0];
				int g = colorArray[1];
				int b = colorArray[2];
				palret[pali] = (r * 1000000) + (g * 1000) + b; // add to palette as RRRGGGBBB
				pali++; // increment palette index
			}
			if (pali == 64) {
				break;
			}
		}
		// short palettes throw an error
		if (pali < 16 ) {
			throw controller.new ShortPaletteException("Only " + pali + " colors were found.");
		}
		// truncate long palettes
		int[] newret = new int[64];
		pali = 16 * (pali / 16);
		if (pali > 64) {
			pali = 64;
		}
		for (int i = 0; i < pali; i++) {
			newret[i] = palret[i];
		}
		if (pali < 64) {
			for (int i = pali; i < 64; i++) {
				newret[i] = palret[i%16];
			}
		}
		return newret;
	}

	/**
	 * Reads a Paint.NET palette (<tt>.txt</tt>) for colors.
	 * This method must be separate as Paint.NET uses HEX values to write colors.
	 * <br><br>
	 * This function firsts find as many colors as it can from the palette.
	 * Once the palette is fully read, the number of colors recognized is
	 * rounded down to the nearest multiple of 16.
	 * Each multiple of 16 represents one of Link's mail palettes
	 * (green, blue, red, bunny).
	 * If fewer than 4 palettes are found, any empty palette is copied from green mail.
	 * 
	 * @param pal - Palette to read
	 * @return <b>int[]</b> of 64 colors as integers (RRRGGGBBB) of 64 colors as integers (RRRGGGBBB)
	 * @throws ShortPaletteException Halts the process if enough colors are not found.
	 */
	public static int[] getPaletteColorsFromPaintNET(BufferedReader pal)
			throws NumberFormatException, IOException, ShortPaletteException {
		int[] palret = new int[64];
		String line;

		// read palette
		int pali = 0;
		while ( (line = pal.readLine()) != null) {
			if (line.matches("[0-9A-F] {8}")) {
				char[] line2 = line.toCharArray();
				// read RGB bytes as ints
				int r = Integer.parseInt( ("" + line2[2] + line2[3]), 16);
				int g = Integer.parseInt( ("" + line2[4] + line2[5]), 16);
				int b = Integer.parseInt( ("" + line2[6] + line2[7]), 16);
				palret[pali] = (r * 1000000) + (g * 1000) + b; // add to palette as RRRGGGBBB
				pali++; // increment palette index
			}
			if (pali == 64) {
				break;
			}
		}
		// Paint.NET forces 96 colors, but put this here just in case
		if (pali < 16 ) {
			throw controller.new ShortPaletteException("Only " + pali + " colors were found.");
		}
		// truncate long palettes
		int[] newret = new int[64];
		pali = 16 * (pali / 16);
		if (pali > 64) {
			pali = 64;
		}
		for (int i = 0; i < pali; i++) {
			newret[i] = palret[i];
		}
		if (pali < 64) {
			for (int i = pali; i < 64; i++) {
				newret[i] = palret[i%16];
			}
		}
		return palret;
	}

	/**
	 * Rounds every byte in an image to the nearest 8.
	 * @param raster - image raster to round
	 */
	public static byte[] roundRaster(byte[] raster) {
		byte[] ret = new byte[raster.length];
		for (int i = 0; i < raster.length; i++) {
			int v = (raster[i]+256) % 256;
			v = (v / 8) * 8;
			ret[i] = (byte) v;
		}
		return ret;
	}

	/**
	 * Takes every color in a palette and rounds each byte to the nearest 8.
	 * @param pal - palette to round
	 */
	public static int[] roundPalette(int[] pal) {
		int[] ret = new int[pal.length];
		for (int i = 0; i < pal.length; i++) {
			int color = pal[i];
			int r = color / 1000000;
			int g = (color % 1000000) / 1000;
			int b = color % 1000;
			r = (r / 8) * 8;
			g = (g / 8) * 8;
			b = (b / 8) * 8;
			ret[i] = (r * 1000000) + (g * 1000) + b;
		}
		return ret;
	}
	/**
	 * Extracts palette colors from last 8x8 block of the image.
	 * Each row of this 8x8 block represents one-half of a mail palette.
	 * Row 1 contains green mail's colors 0x0&ndash;0x7;
	 * row 2 contains green mail's colors 0x8&ndash;0xF; etc.
	 * <br><br>
	 * If any pixel of the latter 3 mails matches the color at {0,0}
	 * (green mail's transparent pixel),
	 * it will be replaced with the corresponding color at green mail for that palette's index.
	 * This is done as an attempt to completely fill out all 64 colors of the palette.
	 * @param pixels - image raster, assumed ABGR
	 * @return
	 */
	public static int[] palExtract(byte[] pixels) {
		int[] palret = new int[64];
		int pali = 0;
		int startAt = (128 * 448 - 8) - (128 * 7);
		int endAt = startAt + (8 * 128);
		for (int i = startAt; i < endAt; i+= 128) {
			for (int j = 0; j < 8; j++) {
				int k = i + j;
				int b = (pixels[k*4+1]+256)%256;
				int g = (pixels[k*4+2]+256)%256;
				int r = (pixels[k*4+3]+256)%256;
				palret[pali] = (1000000 * r) + (1000 * g) + b;
				pali++;
			}
		}

		// fill out the palette by removing empty indices
		for (int i = 16; i < palret.length; i++) {
			if (palret[i] == palret[0]) {
				palret[i] = palret[i%16];
			}
		}

		return palret;
	}

	/**
	 * Create binary palette data for appending to the end of the <tt>.spr</tt> file.
	 * @param pal - <b>int[]</b> containined the palette colors as RRRGGGBBB
	 * @return <b>byte[]<b> containing palette data in 5:5:5 format
	 */
	public static byte[] palDataFromArray(int[] pal) {
		// create palette data as 5:5:5
		ByteBuffer palRet = ByteBuffer.allocate(0x80);

		for (int i = 0; i < 16; i++) {
			for (int t = 0; t < 4; t++) {
				int r = pal[i+16*t] / 1000000;
				int g = (pal[i+16*t] % 1000000) / 1000;
				int b = pal[i+16*t] % 1000;
				short s = (short) ((( b / 8) << 10) | ((( g / 8) << 5) | ((( r / 8) << 0))));
				// put color into every mail palette
				palRet.putShort(30*t+i*2,Short.reverseBytes(s));
			}
		}

		// end palette
		return palRet.array();
	}

	public static int[] palFromBinary(byte[] pal) {
		int[] ret = new int[64];
		for (int i = 0; i < 64; i++) {
			short color = 0;
			int pos = (i * 2) + 4;
			color = (short) unsignByte(pal[pos+1]);
			color <<= 8;
			color |= (short) unsignByte(pal[pos]);
			
			byte r = (byte) (((color >> 0) & 0x1F) << 3);
			byte g= (byte) (((color >> 5) & 0x1F) << 3);
			byte b = (byte) (((color >> 10) & 0x1F) << 3);

			int r2 = unsignByte(r);
			int g2 = unsignByte(g);
			int b2 = unsignByte(b);
			ret[i] = (r2 * 1000000) + (g2 * 1000) + b2;
			System.out.println(ret[i]);
		}
		return ret;
	}
	/**
	 * Turn the image into an array of 8x8 blocks.
	 * Assumes ABGR color space.
	 * <br><br>
	 * If a color matches an index that belongs to one of the latter 3 mails
	 * but does not match anything in green mail
	 * then it is treated as the color at the corresponding index of green mail.
	 * 
	 * @param pixels - aray of color indices
	 * @param pal - palette colors
	 * @return <b>byte[][][]</b> representing the image as a grid of color indices
	 */
	public static byte[][][] get8x8(byte[] pixels, int[] pal) {
		int dis = pixels.length/4;
		int largeCol = 0;
		int intRow = 0;
		int intCol = 0;
		int index = 0;

		// all 8x8 squares, read left to right, top to bottom
		byte[][][] eightbyeight = new byte[896][8][8];

		// read image
		for (int i = 0; i < dis; i++) {
			// get each color and get rid of sign
			// colors are stored as {A,B,G,R,A,B,G,R...}
			int b = unsignByte(pixels[i*4+1]);
			int g = unsignByte(pixels[i*4+2]);
			int r = unsignByte(pixels[i*4+3]);

			// convert to 9 digits
			int rgb = (1000000 * r) + (1000 * g) + b;

			// find palette index of current pixel
			for (int s = 0; s < pal.length; s++) {
				if (pal[s] == rgb) {
					eightbyeight[index][intRow][intCol] = (byte) (s % 16); // mod 16 in case it reads another mail
					break;
				}
			}

			// count up square by square
			// at 8, reset the "Interior column" which we use to locate the pixel in 8x8
			// increments the "Large column", which is the index of the 8x8 sprite on the sheet
			// at 16, reset the index and move to the next row
			// (so we can wrap around back to our old 8x8)
			// after 8 rows, undo the index reset, and move on to the next super row
			intCol++;
			if (intCol == 8) {
				index++;
				largeCol++;
				intCol = 0;
				if (largeCol == 16) {
					index -= 16;
					largeCol = 0;
					intRow++;
					if (intRow == 8) {
						index += 16;
						intRow = 0;
					}
				}
			}
		}
		return eightbyeight;
	}

	/**
	 * Converts an index map into a proper 4BPP (SNES) byte map.
	 * @param eightbyeight - color index map
	 * @param pal - palette
	 * @param rando - palette indices to randomize
	 * @return new byte array in SNES4BPP format
	 */
	public static byte[] exportPNG(byte[][][] eightbyeight, byte[] palData, byte[] rando) {
		// why is this here
		// randomize desired indices
		for (int i = 0; i < eightbyeight.length; i++) {
			for (int j = 0; j < eightbyeight[0].length; j++) {
				for (int k = 0; k < eightbyeight[0][0].length; k++) {
					for (byte a : rando) {
						if (eightbyeight[i][j][k] == a) {
							eightbyeight[i][j][k] = (byte) (Math.random() * 16);
						}
					}
				}
			}
		}

		// format of snes 4bpp {row (r), bit plane (b)}
		// bit plane 0 indexed such that 1011 corresponds to 0123
		int bppi[][] = {
				{0,0},{0,1},{1,0},{1,1},{2,0},{2,1},{3,0},{3,1},
				{4,0},{4,1},{5,0},{5,1},{6,0},{6,1},{7,0},{7,1},
				{0,2},{0,3},{1,2},{1,3},{2,2},{2,3},{3,2},{3,3},
				{4,2},{4,3},{5,2},{5,3},{6,2},{6,3},{7,2},{7,3}
		};

		// bit map
		boolean[][][] fourbpp = new boolean[896][32][8];

		for (int i = 0; i < fourbpp.length; i++) {
			// each byte, as per bppi
			for (int j = 0; j < fourbpp[0].length; j++) {
				for (int k = 0; k < 8; k++) {
					// get row r's bth bit plane, based on index j of bppi
					int row = bppi[j][0];
					int plane = bppi[j][1];
					int byteX = eightbyeight[i][row][k];
					// AND the bits with 1000, 0100, 0010, 0001 to get bit in that location
					boolean bitB = ( byteX & (1 << plane) ) > 0;
					fourbpp[i][j][k] = bitB;
				}
			}
		}

		// byte map
		// includes the size of the sheet (896*32) + palette data (0x78)
		byte[] bytemap = new byte[896*32+0x78];

		int k = 0;
		for (int i = 0; i < fourbpp.length; i++) {
			for (int j = 0; j < fourbpp[0].length; j++) {
				byte next = 0;
				// turn true false into byte
				for (boolean a : fourbpp[i][j]) {
					next <<= 1;
					next |= (a ? 1 : 0);
				}
				bytemap[k] = next;
				k++;
			}
		}
		// end 4BPP

		// add palette data, starting at end of sheet
		int i = 896*32-2;
		for (byte b : palData) {
			if (i == bytemap.length) {
				break;
			}
			bytemap[i] = b;
			i++;
		}
		return bytemap;
	}

	/**
	 * Writes the image to an <tt>.spr</tt> file.
	 * @param map - SNES 4BPP file, including 5:5:5
	 * @param loc - File path of exported sprite
	 */
	public static void writeSPR(byte[] map, String loc) throws IOException {
		// create a file at directory
		new File(loc);

		FileOutputStream fileOuputStream = new FileOutputStream(loc);
		try {
			fileOuputStream.write(map);
		} finally {
			fileOuputStream.close();
		}
	}

	public static int unsignByte(byte b) {
		int ret = ((b + 256) % 256);
		return ret;
	}
	// errors

	/**
	 *  Palette has <16 colors
	 */
	public class ShortPaletteException extends Exception {
		private static final long serialVersionUID = 1L;
		public ShortPaletteException(String message) {
			super(message);
		}

		public ShortPaletteException() {}
	}

	/**
	 *  Image is wrong dimensions
	 */
	public class BadDimensionsException extends Exception {
		private static final long serialVersionUID = 1L;
		public BadDimensionsException(String message) {
			super(message);
		}

		public BadDimensionsException() {}
	}
}