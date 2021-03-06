// Copyright (c) 2010 Ryan Seal <rlseal -at- gmail.com>
//
// This file is part of GnuRadar Software.
//
// GnuRadar is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// GnuRadar is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with GnuRadar.  If not, see <http://www.gnu.org/licenses/>.
package com.gnuradar.run;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;

import org.yaml.snakeyaml.Yaml;

import com.corejava.GBC;
import com.gnuradar.common.FixedFrame;
import com.gnuradar.proto.Status.StatusMessage;
import com.gnuradar.run.ButtonPanel.State;

public class Run implements ActionListener, PropertyChangeListener {

	// define constants
	private static final int DEFAULT_WIDTH = 575;
	private static final int DEFAULT_HEIGHT = 230;
	private static final String TITLE = "GnuRadarRun";
	private static final String VERSION = "Version: 1.0.0";
	private static final String BUILD = "Build: September 15, 2012";
	private static final String COPYRIGHT = "Copyright: \u00a9 2009-2012";
	private static final String AUTHOR = "Author: Ryan Seal";
	private static final String RC_FILE = ".gradarrc";

	private String statusIpAddress;
	private String controlIpAddress;

	private StatusListener statusListener;
	private JLabel statusLabel;
	private JTextPane statusPane;
	private ProgressPanel progressPanel;
	private Document statusDocument;

	private static ButtonPanel buttonPanel;
	private static FixedFrame frame;
	private static String userNode = "/com/gnuradar/run";

	private Thread thread = null;
	private StatusThread statusThread = null;

	private JMenuItem quitAction;
	private JMenuItem loadAction;
	private JMenuItem aboutAction;
	private JMenuItem plotAction;
	private JMenuItem bpgAction;
	private JMenuItem configureAction;

	private void loadPreferences() {
		Preferences preferences = Preferences.userRoot().node(userNode);
		int x = preferences.getInt("x", 0);
		int y = preferences.getInt("y", 0);
		File file = new File(preferences.get("config_dir", ""));
		buttonPanel.setConfigurationFile(file);
		frame.setLocation(x, y);
	}

	private void savePreferences() {
		Point point = frame.getLocation();
		File file = buttonPanel.getConfigurationFile();

		Preferences preferences = Preferences.userRoot().node(userNode);
		preferences.put("x", Integer.toString(point.x));
		preferences.put("y", Integer.toString(point.y));
		preferences.put("config_dir", file.toString());
	}

	private void updateDisplay(StatusMessage msg) {

		progressPanel.setHead(msg.getHead());
		progressPanel.setTail(msg.getTail());
		progressPanel.setDepth(msg.getDepth());
		progressPanel.setNumBuffers(20);
	}

	private static void setComponentSize(JComponent obj, Dimension dimension) {
		obj.setMinimumSize(dimension);
		obj.setPreferredSize(dimension);
	}

	// main entry point
	public static void main(String[] args) {
		// location of the rc file
		File rcFile = new File(System.getProperty("user.home")
				+ System.getProperty("file.separator") + RC_FILE);
		System.out.println("Opening file " + rcFile.toString());

		final Run run = new Run();

		try {

			FileInputStream fin = new FileInputStream(rcFile);
			Yaml yaml = new Yaml();

			@SuppressWarnings("unchecked")
			Map<String, Object> data = (Map<String, Object>) yaml.load(fin);
			fin.close();

			run.statusIpAddress = (String) data.get("status");
			run.controlIpAddress = (String) data.get("control");

		} catch (UnknownHostException e1) {
			System.out.println(" Could not contact the specified IP address ");
			e1.printStackTrace();
		} catch (IOException e) {
			System.out.println(" Could not locate the rc file "
					+ rcFile.toString());
			e.printStackTrace();
		}

		// this is required for proper event-handling
		EventQueue.invokeLater(new Runnable() {
			public void run() {

				// use the grid bag layout manager
				GridBagLayout gridBagLayout = new GridBagLayout();

				Border border = BorderFactory.createEtchedBorder();
				TitledBorder tBorder = BorderFactory.createTitledBorder(border,
						"Status");
				tBorder.setTitleJustification(TitledBorder.CENTER);

				run.statusPane = new JTextPane();
				run.statusDocument = new DefaultStyledDocument();
				run.statusPane.setBorder(tBorder);
				run.statusPane.setEditable(false);
				run.statusPane.setDocument(run.statusDocument);

				setComponentSize(run.statusPane, new Dimension(300, 100));

				JPanel statusPanel = new JPanel();
				statusPanel.setBorder(border);

				run.statusLabel = new JLabel("UNCONFIGURED", JLabel.CENTER);
				run.statusLabel.setFont(new Font("", Font.BOLD, 16));
				run.statusLabel.setForeground(Color.WHITE);

				setComponentSize(statusPanel, new Dimension(400, 25));

				statusPanel.setBackground(Color.BLUE);
				statusPanel.add(run.statusLabel);

				buttonPanel = new ButtonPanel();
				buttonPanel.setIpAddress(run.controlIpAddress);
				setComponentSize(buttonPanel, new Dimension(100, 100));

				run.progressPanel = new ProgressPanel();
				setComponentSize(run.progressPanel, new Dimension(400, 50));

				// create menu bar and menu items
				JMenuBar menuBar = new JMenuBar();

				run.loadAction = new JMenuItem("Load", 'L');
				run.loadAction.addActionListener(run);
				run.quitAction = new JMenuItem("Quit", 'Q');
				run.quitAction.addActionListener(run);
				run.plotAction = new JMenuItem("Plotter", 'P');
				run.plotAction.addActionListener(run);
				run.bpgAction = new JMenuItem("BitPatternGenerator", 'B');
				run.bpgAction.addActionListener(run);
				run.aboutAction = new JMenuItem("About", 'A');
				run.aboutAction.addActionListener(run);
				run.configureAction = new JMenuItem("GnuRadar Configure", 'C');
				run.configureAction.addActionListener(run);

				JMenu fileMenu = new JMenu("File");
				fileMenu.add(run.loadAction);
				fileMenu.addSeparator();
				fileMenu.add(run.quitAction);

				JMenu toolMenu = new JMenu("Tools");
				toolMenu.add(run.plotAction);
				toolMenu.add(run.bpgAction);
				toolMenu.add(run.configureAction);

				JMenu helpMenu = new JMenu("Help");
				helpMenu.add(run.aboutAction);

				// TODO: Enable these when ready
				run.plotAction.setEnabled(false);
				run.bpgAction.setEnabled(false);

				menuBar.add(fileMenu);
				menuBar.add(toolMenu);
				menuBar.add(Box.createHorizontalGlue());
				menuBar.add(helpMenu);

				// create main window frame and set properties.
				frame = new FixedFrame(DEFAULT_WIDTH, DEFAULT_HEIGHT, TITLE
						+ " " + VERSION);
				frame.setLayout(gridBagLayout);
				frame.setJMenuBar(menuBar);
				frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

				// make sure that a click to 'x' out the window passes through
				// the quit function to ensure proper shutdown.
				frame.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						run.quit();
					}
				});

				frame.add(buttonPanel, new GBC(0, 1, 10, 100).setIpad(5, 5)
						.setSpan(1, 3).setFill(GridBagConstraints.VERTICAL));
				frame.add(statusPanel, new GBC(0, 0, 10, 10).setIpad(5, 5)
						.setSpan(4, 1).setFill(GridBagConstraints.HORIZONTAL));
				frame.add(run.statusPane, new GBC(1, 1, 100, 100).setIpad(5, 5)
						.setSpan(3, 1).setFill(GridBagConstraints.HORIZONTAL));
				frame.add(run.progressPanel, new GBC(1, 3, 100, 100).setIpad(5, 5)
						.setSpan(3, 1).setFill(GridBagConstraints.HORIZONTAL));

				run.loadPreferences();

				buttonPanel.addPropertyChangeListener(run);
				frame.setVisible(true);
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();

		if (source == configureAction) {

			ProcessBuilder pBuilder = new ProcessBuilder("gradar-configure");
			try {
				Process process = pBuilder.start();
				process.waitFor();
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (InterruptedException e2) {
				e2.printStackTrace();
			}
		}
		if (source == loadAction) {
			buttonPanel.clickLoadButton();
		}

		if (source == quitAction) {
			quit();
		}

		if (source == aboutAction) {
			JOptionPane.showMessageDialog(null, TITLE + "\n" + VERSION + "\n"
					+ BUILD + "\n" + AUTHOR + "\n" + COPYRIGHT + "\n");
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		State state = buttonPanel.getState();
		statusLabel.setText(state.getValue());

		if (state == State.RUNNING) {
			System.out.println("Starting Status Thread");
			statusThread = new StatusThread(this.statusIpAddress);
			thread = new Thread(statusThread);
			thread.start();

			statusListener = new StatusListener() {
				@Override
				public void eventOccurred(StatusEvent event) {
					updateDisplay((StatusMessage) event.getSource());
				}
			};
			statusThread.addStatusListener(statusListener);
		}

		if (state == State.STOPPED && thread.isAlive()) {

			statusPane.setText("System Stopped.");
			statusThread.removeStatusListener(statusListener);
			statusThread.stopStatus();
			
			try {
				thread.join();
			} catch (InterruptedException e) {
				statusPane.setText("Status thread join was interrupted.");
			}
		}

		if (state == State.CONFIGURED) {
			statusPane.setText("Configuration File Loaded.");
		}
		
		if (state == State.ERROR){
			statusPane.setText(buttonPanel.getServerResponse());
		}
	}

	public void quit() {
		if (buttonPanel.getState() == State.RUNNING) {
			JOptionPane.showMessageDialog(null,
					"System is currently in operation. Press <Stop> before"
							+ " attempting to exit");
		} else {
			savePreferences();
			System.exit(0);
		}
	}
}
