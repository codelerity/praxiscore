
module org.praxislive.code.services {
    
    requires java.compiler;
    requires java.logging;
    
    requires org.praxislive.base;
    requires org.praxislive.core;
    requires org.praxislive.code;
    requires org.praxislive.script;

    provides org.praxislive.core.RootHub.ExtensionProvider with
            org.praxislive.code.services.CodeServicesExtensionProvider;
    
    uses org.praxislive.core.services.ComponentFactoryProvider;
    uses org.praxislive.code.LibraryResolver.Provider;
    uses org.praxislive.code.LibraryResolver.SystemInfo;
    
}
