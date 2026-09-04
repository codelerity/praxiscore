
module org.praxislive.code {

    requires java.compiler;

    requires transitive org.praxislive.core;
    requires org.praxislive.base;
    requires org.praxislive.script;
    
    exports org.praxislive.code;
    exports org.praxislive.code.userapi;

    provides javax.annotation.processing.Processor with 
            org.praxislive.code.internal.GenerateTemplateProcessor;
    provides org.praxislive.core.Port.TypeProvider with
            org.praxislive.code.internal.CodePortTypeProvider;
    provides org.praxislive.core.Protocol.TypeProvider with
            org.praxislive.code.internal.CodeProtocolsProvider;
    provides org.praxislive.script.CommandInstaller with
            org.praxislive.code.internal.CodeCommands;
    
    uses org.praxislive.code.CodeConnector.Plugin;
    
}
