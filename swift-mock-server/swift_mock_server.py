#!/usr/bin/env python3
"""
SWIFT MT103 Mock Server
Simulates a bank's back-office system receiving SWIFT messages over TCP.

This mock server:
1. Listens on TCP port 10103
2. Parses incoming MT103 messages using regex
3. Validates message structure
4. Sends ACK (F21) or NACK responses
5. Logs all transactions

Usage:
    python swift_mock_server.py [--port 10103] [--host 0.0.0.0]

Author: MuleSoft Financial Services Team
Version: 1.0.0
"""

import socket
import threading
import re
import logging
import argparse
import json
from datetime import datetime
from typing import Dict, List, Optional, Tuple

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('swift_mock_server.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger('SWIFT-Mock-Server')


class SwiftMessage:
    """Represents a parsed SWIFT message"""
    
    def __init__(self, raw_message: str):
        self.raw = raw_message
        self.blocks = self._parse_blocks()
        self.fields = self._parse_fields()
        self.is_valid = self._validate()
        
    def _parse_blocks(self) -> Dict[str, str]:
        """Parse SWIFT message blocks {1:...} {2:...} {4:...}"""
        blocks = {}
        block_pattern = r'\{(\d):(.*?)\}'
        matches = re.finditer(block_pattern, self.raw, re.DOTALL)
        
        for match in matches:
            block_num = match.group(1)
            block_content = match.group(2)
            blocks[f'block{block_num}'] = block_content
            
        return blocks
    
    def _parse_fields(self) -> Dict[str, str]:
        """Parse SWIFT fields like :20:, :32A:, etc."""
        fields = {}
        
        # Extract block 4 (message content)
        block4 = self.blocks.get('block4', '')
        
        # Parse fields using pattern :TAG:VALUE
        field_pattern = r':(\d+[A-Z]?):(.*?)(?=:\d+[A-Z]?:|$)'
        matches = re.finditer(field_pattern, block4, re.DOTALL)
        
        for match in matches:
            tag = match.group(1)
            value = match.group(2).strip()
            fields[tag] = value
            
        return fields
    
    def _validate(self) -> bool:
        """Validate MT103 message structure"""
        required_fields = ['20', '32A', '50K', '59']
        
        for field in required_fields:
            if field not in self.fields:
                logger.warning(f"Missing required field: {field}")
                return False
                
        return True
    
    def get_field(self, tag: str) -> Optional[str]:
        """Get field value by tag"""
        return self.fields.get(tag)
    
    def to_dict(self) -> Dict:
        """Convert message to dictionary for logging"""
        return {
            'reference': self.get_field('20'),
            'value_date_amount': self.get_field('32A'),
            'ordering_customer': self.get_field('50K'),
            'beneficiary': self.get_field('59'),
            'is_valid': self.is_valid,
            'fields': self.fields
        }


class SwiftMockServer:
    """SWIFT Mock Server - Simulates bank back-office system"""
    
    def __init__(self, host: str = '0.0.0.0', port: int = 10103):
        self.host = host
        self.port = port
        self.server_socket = None
        self.is_running = False
        self.message_count = 0
        self.sessions = {}
        
    def start(self):
        """Start the SWIFT mock server"""
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        
        try:
            self.server_socket.bind((self.host, self.port))
            self.server_socket.listen(5)
            self.is_running = True
            
            logger.info(f"🚀 SWIFT Mock Server started on {self.host}:{self.port}")
            logger.info(f"📡 Ready to receive MT103 messages...")
            logger.info(f"💡 Press Ctrl+C to stop")
            
            while self.is_running:
                try:
                    client_socket, client_address = self.server_socket.accept()
                    logger.info(f"✅ New connection from {client_address}")
                    
                    # Handle client in separate thread
                    client_thread = threading.Thread(
                        target=self._handle_client,
                        args=(client_socket, client_address)
                    )
                    client_thread.daemon = True
                    client_thread.start()
                    
                except KeyboardInterrupt:
                    logger.info("\n🛑 Server shutdown requested")
                    break
                except Exception as e:
                    logger.error(f"Error accepting connection: {e}")
                    
        finally:
            self.stop()
    
    def stop(self):
        """Stop the server"""
        self.is_running = False
        if self.server_socket:
            self.server_socket.close()
        logger.info("👋 SWIFT Mock Server stopped")
    
    def _handle_client(self, client_socket: socket.socket, client_address: Tuple):
        """Handle client connection"""
        session_id = f"SESSION-{len(self.sessions) + 1}"
        self.sessions[session_id] = {
            'address': client_address,
            'connected_at': datetime.now().isoformat(),
            'messages_received': 0
        }
        
        logger.info(f"📋 Session created: {session_id}")
        
        try:
            # Send welcome/authentication response
            self._send_auth_response(client_socket, session_id)
            
            buffer = ""
            while self.is_running:
                data = client_socket.recv(4096)
                
                if not data:
                    logger.info(f"🔌 Client {client_address} disconnected")
                    break
                
                buffer += data.decode('utf-8', errors='ignore')
                
                # Check if we have a complete SWIFT message
                if self._is_complete_message(buffer):
                    self._process_message(client_socket, buffer, session_id)
                    buffer = ""
                    
        except Exception as e:
            logger.error(f"Error handling client {client_address}: {e}")
        finally:
            client_socket.close()
            logger.info(f"Session {session_id} closed")
    
    def _send_auth_response(self, client_socket: socket.socket, session_id: str):
        """Send authentication response"""
        auth_response = f"AUTH_OK:SWIFT_MOCK_SERVER:{session_id}\n"
        client_socket.send(auth_response.encode('utf-8'))
        logger.info(f"✉️  Sent authentication response for {session_id}")
    
    def _is_complete_message(self, buffer: str) -> bool:
        """Check if buffer contains complete SWIFT message"""
        return ('{1:' in buffer and 
                '{2:' in buffer and 
                '{4:' in buffer and 
                ('-}' in buffer or buffer.count('}') >= 3))
    
    def _process_message(self, client_socket: socket.socket, message: str, session_id: str):
        """Process received SWIFT message"""
        self.message_count += 1
        msg_id = f"MSG-{self.message_count:06d}"
        
        logger.info(f"📨 Received message {msg_id} in session {session_id}")
        logger.debug(f"Raw message:\n{message}")
        
        try:
            # Parse SWIFT message
            swift_msg = SwiftMessage(message)
            
            logger.info(f"🔍 Parsed MT103 message:")
            logger.info(f"   Reference: {swift_msg.get_field('20')}")
            logger.info(f"   Value/Amount: {swift_msg.get_field('32A')}")
            logger.info(f"   Ordering Customer: {swift_msg.get_field('50K')}")
            logger.info(f"   Beneficiary: {swift_msg.get_field('59')}")
            
            # Validate and send response
            if swift_msg.is_valid:
                self._send_ack(client_socket, msg_id, swift_msg)
                self.sessions[session_id]['messages_received'] += 1
            else:
                self._send_nack(client_socket, msg_id, "Invalid message structure")
                
            # Log transaction
            self._log_transaction(msg_id, session_id, swift_msg)
            
        except Exception as e:
            logger.error(f"Error processing message: {e}")
            self._send_nack(client_socket, msg_id, str(e))
    
    def _send_ack(self, client_socket: socket.socket, msg_id: str, swift_msg: SwiftMessage):
        """Send ACK (F21 - Positive Acknowledgment)"""
        reference = swift_msg.get_field('20') or msg_id
        
        # Simplified ACK format
        ack = f"{{1:F21MOCKSVRXXXXAXXX0000000000}}"
        ack += f"{{2:I901MOCKRCVRXXXXN}}"
        ack += f"{{4:\n:20:{reference}\n:77E:ACK\n-}}"
        
        client_socket.send(ack.encode('utf-8'))
        logger.info(f"✅ Sent ACK for message {msg_id}")
    
    def _send_nack(self, client_socket: socket.socket, msg_id: str, reason: str):
        """Send NACK (Negative Acknowledgment)"""
        
        nack = f"{{1:F21MOCKSVRXXXXAXXX0000000000}}"
        nack += f"{{2:I901MOCKRCVRXXXXN}}"
        nack += f"{{4:\n:20:{msg_id}\n:77E:NACK\n:79:{reason}\n-}}"
        
        client_socket.send(nack.encode('utf-8'))
        logger.warning(f"❌ Sent NACK for message {msg_id}: {reason}")
    
    def _log_transaction(self, msg_id: str, session_id: str, swift_msg: SwiftMessage):
        """Log transaction to file"""
        transaction = {
            'message_id': msg_id,
            'session_id': session_id,
            'timestamp': datetime.now().isoformat(),
            'message_details': swift_msg.to_dict()
        }
        
        with open('swift_transactions.log', 'a') as f:
            f.write(json.dumps(transaction) + '\n')


def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(
        description='SWIFT MT103 Mock Server',
        formatter_class=argparse.RawDescriptionHelpFormatter
    )
    
    parser.add_argument('--host', default='0.0.0.0', help='Host to bind to')
    parser.add_argument('--port', type=int, default=10103, help='Port to listen on')
    parser.add_argument('--debug', action='store_true', help='Enable debug logging')
    
    args = parser.parse_args()
    
    if args.debug:
        logging.getLogger().setLevel(logging.DEBUG)
    
    print("""
╔═══════════════════════════════════════════════════════════╗
║         SWIFT MT103 Mock Server v1.0.0                    ║
║         Simulating Bank Back-Office System                ║
╚═══════════════════════════════════════════════════════════╝
    """)
    
    server = SwiftMockServer(host=args.host, port=args.port)
    
    try:
        server.start()
    except KeyboardInterrupt:
        logger.info("\n🛑 Shutting down gracefully...")
        server.stop()


if __name__ == '__main__':
    main()

