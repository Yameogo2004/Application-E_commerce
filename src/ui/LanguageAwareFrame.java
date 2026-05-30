package ui;

import javax.swing.*;

public abstract class LanguageAwareFrame extends JFrame implements LanguageManager.LanguageChangeListener {

    public LanguageAwareFrame() {
        LanguageManager.getInstance().addLanguageChangeListener(this);
    }

    @Override
    public void dispose() {
        LanguageManager.getInstance().removeLanguageChangeListener(this);
        super.dispose();
    }

    public abstract void refreshTexts();

    @Override
    public void onLanguageChanged() {
        SwingUtilities.invokeLater(this::refreshTexts);
    }
}