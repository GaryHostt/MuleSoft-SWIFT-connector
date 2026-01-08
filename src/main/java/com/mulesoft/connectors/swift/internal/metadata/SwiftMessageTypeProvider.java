package com.mulesoft.connectors.swift.internal.metadata;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * SWIFT Inspector - Message Type Value Provider
 * 
 * <h2>Professional Pattern: Dynamic Dropdowns in Anypoint Studio</h2>
 * 
 * <p><strong>Problem with AI-Generated Connectors</strong>:</p>
 * <pre>{@code
 * // Developer must manually type message type (error-prone)
 * <swift:parse-message messageType="MT103" />  <!-- Typo: "MT130" -->
 * }</pre>
 * 
 * <p><strong>Professional Solution (ValueProvider)</strong>:</p>
 * <pre>{@code
 * // Anypoint Studio shows DROPDOWN with all available message types
 * // User clicks → selects "MT103 - Single Customer Credit Transfer"
 * // No typos possible!
 * }</pre>
 * 
 * <h2>How It Works in Studio</h2>
 * <ol>
 *   <li>User drags SWIFT connector operation to canvas</li>
 *   <li>Clicks on "Message Type" parameter</li>
 *   <li>Studio calls this {@code ValueProvider} to populate dropdown</li>
 *   <li>User sees: "MT103 - Single Customer Credit Transfer", "MT940 - Customer Statement", etc.</li>
 *   <li>User selects from dropdown (no manual typing)</li>
 * </ol>
 * 
 * <h2>Benefits</h2>
 * <ul>
 *   <li>✅ <strong>No Typos</strong>: Impossible to enter "MT130" when "MT103" is meant</li>
 *   <li>✅ <strong>Discoverability</strong>: Developers see all supported message types</li>
 *   <li>✅ <strong>Documentation</strong>: Each type has a description shown in UI</li>
 *   <li>✅ <strong>Professional UX</strong>: Matches standard MuleSoft connectors (Salesforce, SAP)</li>
 * </ul>
 * 
 * <h2>Usage in Operation</h2>
 * <pre>{@code
 * @DisplayName("Parse SWIFT Message")
 * public SwiftMessage parseMessage(
 *     @Connection SwiftConnection connection,
 *     @OfValues(SwiftMessageTypeProvider.class)  // ← This annotation
 *     @DisplayName("Message Type")
 *     String messageType) {
 *     // ...
 * }
 * }</pre>
 * 
 * @see <a href="https://docs.mulesoft.com/mule-sdk/latest/value-providers">MuleSoft SDK Value Providers</a>
 */
public class SwiftMessageTypeProvider implements ValueProvider {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SwiftMessageTypeProvider.class);
    
    /**
     * Resolve available SWIFT message types.
     * 
     * <p>Called by Anypoint Studio when user clicks on parameter dropdown.
     * Returns a set of values to populate the dropdown list.</p>
     * 
     * @return Set of message types with IDs and display names
     * @throws ValueResolvingException If value resolution fails
     */
    @Override
    public Set<Value> resolve() throws ValueResolvingException {
        LOGGER.debug("Resolving SWIFT message types for Studio dropdown");
        
        try {
            ValueBuilder builder = ValueBuilder.getValuesFor("SWIFT Message Types");
            
            // ========== MT (FIN) Messages ==========
            
            // Category 1: Customer Payments and Cheques
            builder.newValue("MT103")
                    .withDisplayName("MT103 - Single Customer Credit Transfer")
                    .build();
            
            builder.newValue("MT110")
                    .withDisplayName("MT110 - Advice of Cheque(s)")
                    .build();
            
            // Category 2: Financial Institution Transfers
            builder.newValue("MT200")
                    .withDisplayName("MT200 - Financial Institution Transfer for Own Account")
                    .build();
            
            builder.newValue("MT202")
                    .withDisplayName("MT202 - General Financial Institution Transfer")
                    .build();
            
            builder.newValue("MT202COV")
                    .withDisplayName("MT202COV - General FI Transfer (Cover Method)")
                    .build();
            
            builder.newValue("MT205")
                    .withDisplayName("MT205 - Financial Institution Transfer Execution")
                    .build();
            
            // Category 3: Treasury Markets - Foreign Exchange
            builder.newValue("MT300")
                    .withDisplayName("MT300 - Foreign Exchange Confirmation")
                    .build();
            
            builder.newValue("MT304")
                    .withDisplayName("MT304 - Advice/Instruction of Third Party Deal")
                    .build();
            
            // Category 4: Collections and Cash Letters
            builder.newValue("MT400")
                    .withDisplayName("MT400 - Advice of Payment")
                    .build();
            
            builder.newValue("MT410")
                    .withDisplayName("MT410 - Acknowledgement")
                    .build();
            
            // Category 5: Securities Markets
            builder.newValue("MT515")
                    .withDisplayName("MT515 - Client Confirmation of Purchase or Sale")
                    .build();
            
            builder.newValue("MT535")
                    .withDisplayName("MT535 - Statement of Holdings")
                    .build();
            
            builder.newValue("MT540")
                    .withDisplayName("MT540 - Receive Free")
                    .build();
            
            builder.newValue("MT541")
                    .withDisplayName("MT541 - Receive Against Payment")
                    .build();
            
            builder.newValue("MT542")
                    .withDisplayName("MT542 - Deliver Free")
                    .build();
            
            builder.newValue("MT543")
                    .withDisplayName("MT543 - Deliver Against Payment")
                    .build();
            
            // Category 9: Cash Management and Customer Status
            builder.newValue("MT900")
                    .withDisplayName("MT900 - Confirmation of Debit")
                    .build();
            
            builder.newValue("MT910")
                    .withDisplayName("MT910 - Confirmation of Credit")
                    .build();
            
            builder.newValue("MT940")
                    .withDisplayName("MT940 - Customer Statement Message")
                    .build();
            
            builder.newValue("MT941")
                    .withDisplayName("MT941 - Balance Report")
                    .build();
            
            builder.newValue("MT942")
                    .withDisplayName("MT942 - Interim Transaction Report")
                    .build();
            
            builder.newValue("MT950")
                    .withDisplayName("MT950 - Statement Message")
                    .build();
            
            // System Messages
            builder.newValue("MT011")
                    .withDisplayName("MT011 - Delivery Notification (ACK)")
                    .build();
            
            builder.newValue("MT019")
                    .withDisplayName("MT019 - Abort Notification")
                    .build();
            
            builder.newValue("MT101")
                    .withDisplayName("MT101 - Request for Transfer")
                    .build();
            
            // ========== ISO 20022 (MX) Messages ==========
            
            // pain - Payments Initiation
            builder.newValue("pain.001.001.09")
                    .withDisplayName("pain.001 - CustomerCreditTransferInitiation")
                    .build();
            
            builder.newValue("pain.002.001.10")
                    .withDisplayName("pain.002 - CustomerPaymentStatusReport")
                    .build();
            
            builder.newValue("pain.008.001.08")
                    .withDisplayName("pain.008 - CustomerDirectDebitInitiation")
                    .build();
            
            // pacs - Payments Clearing and Settlement
            builder.newValue("pacs.002.001.10")
                    .withDisplayName("pacs.002 - FIToFIPaymentStatusReport")
                    .build();
            
            builder.newValue("pacs.003.001.08")
                    .withDisplayName("pacs.003 - FIToFICustomerDirectDebit")
                    .build();
            
            builder.newValue("pacs.004.001.09")
                    .withDisplayName("pacs.004 - PaymentReturn")
                    .build();
            
            builder.newValue("pacs.008.001.08")
                    .withDisplayName("pacs.008 - FIToFICustomerCreditTransfer")
                    .build();
            
            builder.newValue("pacs.009.001.08")
                    .withDisplayName("pacs.009 - FinancialInstitutionCreditTransfer")
                    .build();
            
            builder.newValue("pacs.028.001.03")
                    .withDisplayName("pacs.028 - FIToFIPaymentStatusRequest")
                    .build();
            
            // camt - Cash Management
            builder.newValue("camt.052.001.08")
                    .withDisplayName("camt.052 - BankToCustomerAccountReport")
                    .build();
            
            builder.newValue("camt.053.001.08")
                    .withDisplayName("camt.053 - BankToCustomerStatement")
                    .build();
            
            builder.newValue("camt.054.001.08")
                    .withDisplayName("camt.054 - BankToCustomerDebitCreditNotification")
                    .build();
            
            builder.newValue("camt.056.001.08")
                    .withDisplayName("camt.056 - FIToFIPaymentCancellationRequest")
                    .build();
            
            // acmt - Account Management
            builder.newValue("acmt.001.001.07")
                    .withDisplayName("acmt.001 - AccountOpeningInstruction")
                    .build();
            
            builder.newValue("acmt.002.001.07")
                    .withDisplayName("acmt.002 - AccountDetailsConfirmation")
                    .build();
            
            LOGGER.info("Resolved {} SWIFT message types for Studio dropdown", builder.build().size());
            return builder.build();
            
        } catch (Exception e) {
            LOGGER.error("Failed to resolve SWIFT message types", e);
            throw new ValueResolvingException("Failed to load SWIFT message types: " + e.getMessage(),
                    ValueResolvingException.FailureCode.UNKNOWN);
        }
    }
}

