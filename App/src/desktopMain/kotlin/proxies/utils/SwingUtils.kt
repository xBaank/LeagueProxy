package proxies.utils

import javax.swing.JOptionPane

fun showError(msg: String, title: String) =
    JOptionPane.showMessageDialog(null, msg, title, JOptionPane.ERROR_MESSAGE)

fun askForClose(msg: String, title: String) =
    JOptionPane.showConfirmDialog(
        null,
        msg,
        title,
        JOptionPane.YES_NO_OPTION,
        JOptionPane.ERROR_MESSAGE
    ) == JOptionPane.YES_OPTION
