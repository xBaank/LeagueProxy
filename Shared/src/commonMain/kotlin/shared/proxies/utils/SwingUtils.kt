package shared.proxies.utils

import javax.swing.JOptionPane

fun showError(msg: String, title: String) =
    JOptionPane.showMessageDialog(null, msg, title, JOptionPane.ERROR_MESSAGE)