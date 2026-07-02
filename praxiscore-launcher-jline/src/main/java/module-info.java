
module org.praxislive.launcher.jline {
    
    requires org.praxislive.core;
    requires org.praxislive.base;
    requires org.praxislive.launcher;
    requires org.jline.terminal;
    requires org.jline.reader;
    requires org.jline.builtins;
    requires org.jline.prompt;

    provides org.praxislive.launcher.TerminalIOProvider with
            org.praxislive.launcher.jline.JLineTerminalIOProvider;
    provides org.praxislive.launcher.Signals with 
            org.praxislive.launcher.jline.JLineSignals;
    
}
