package com.mulesoft.connectors.swift;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.license.RequiresEnterpriseLicense;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;

import com.mulesoft.connectors.swift.internal.connection.SwiftConnectionProvider;
import com.mulesoft.connectors.swift.internal.error.SwiftErrorType;
import com.mulesoft.connectors.swift.internal.operation.*;
import com.mulesoft.connectors.swift.internal.source.SwiftMessageListener;

/**
 * MuleSoft SWIFT Connector
 * 
 * Enterprise-grade financial messaging connector supporting:
 * - Legacy MT (Message Type) standards
 * - Modern ISO 20022 (MX) standards
 * - SWIFT gpi (Global Payments Innovation)
 * - Full compliance and security features
 * 
 * Designed for Mule 4.10+ with Java 17
 * 
 * @author MuleSoft Financial Services Team
 * @version 1.0.0
 */
@Extension(name = "SWIFT", vendor = "MuleSoft")
@Xml(prefix = "swift")
@JavaVersionSupport({JAVA_17})
@Operations({
    CoreMessagingOperations.class,
    GpiOperations.class,
    TransformationOperations.class,
    SecurityOperations.class,
    SessionOperations.class,
    ErrorHandlingOperations.class,
    ReferenceDataOperations.class,
    ObservabilityOperations.class
})
@ConnectionProviders(SwiftConnectionProvider.class)
@Sources(SwiftMessageListener.class)
@ErrorTypes(SwiftErrorType.class)
@RequiresEnterpriseLicense(allowEvaluationLicense = true)
public class SwiftConnector {

    /**
     * Connector version
     */
    public static final String VERSION = "1.0.0";
    
    /**
     * Connector name
     */
    public static final String NAME = "SWIFT Connector";
    
}

