package osrs.dev.ui;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;

public class Components
{
    public static JLabel createMapView()
    {
        JLabel mapView = new JLabel();
        mapView.setHorizontalAlignment(JLabel.CENTER);
        mapView.setVerticalAlignment(JLabel.CENTER);
        return mapView;
    }

    public static JButton createDirectionButton(Direction direction, ActionListener actionListener)
    {
        JButton button = new JButton(direction.getSymbol());
        button.setPreferredSize(new Dimension(50, 50));
        button.addActionListener(actionListener);
        return button;
    }

    public static JSlider createZoomSlider(ChangeListener changeListener)
    {
        JSlider zoomSlider = new JSlider(JSlider.VERTICAL, 5, 2000, 100);
        zoomSlider.setMajorTickSpacing(100);
        zoomSlider.setPaintTicks(true);
        zoomSlider.setPaintLabels(true);
        zoomSlider.addChangeListener(changeListener);
        return zoomSlider;
    }

    public static JSlider createSpeedSlider()
    {
        JSlider speedSlider = new JSlider(JSlider.HORIZONTAL, 0, 300, 5);
        speedSlider.setMajorTickSpacing(15);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        return speedSlider;
    }
}
