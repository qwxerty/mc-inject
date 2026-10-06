package client.ui;

import client.Client;
import client.module.Module;
import client.module.ModuleManager;

import javax.swing.*;
import java.awt.*;

public class Gui {
    public static void open() {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Client");
            f.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            f.setAlwaysOnTop(true);

            JPanel panel = new JPanel(new GridLayout(0, 1));
            for (Module m : ModuleManager.all()) {
                JCheckBox cb = new JCheckBox(m.name, m.isEnabled());
                cb.addActionListener(e -> m.setEnabled(cb.isSelected()));
                panel.add(cb);
            }
            JButton eject = new JButton("Eject");
            eject.addActionListener(e -> { Client.eject(); f.dispose(); });
            panel.add(eject);

            f.add(panel);
            f.pack();
            f.setLocationByPlatform(true);
            f.setVisible(true);
        });
    }
}
