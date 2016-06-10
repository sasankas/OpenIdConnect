
package com.nri.xxxxxxxx.inf.spi.impl;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.QueueSession;
import javax.jms.TextMessage;

import com.nri.xxxxxxxx.inf.spi.IMessageProtocol;

/**
 * Protocol for handling javx.jms.TextMessage where a message, sent or obtained
 * from a Queue, is packed in flat text convention.
 *
 *
 * @version     $Revision: 1.1 $, $Date: 2004/07/09 12:48:06 $
 *
 *
 * @since       1.0
 *
 */

public class TextMessageProtocol implements IMessageProtocol {

    //~ Constructors ===========================================================

    /**
     * Construct a default instance.
     */
    public TextMessageProtocol() {
    }

    //~ Instance Methods =================================================

    /**
     * {@inheritDoc}.
     */
    public String toString(Message message)
            throws javax.jms.JMSException {

        TextMessage tm = (TextMessage) message;
        return tm.getText();

    }

    /**
     * {@inheritDoc}.
     */
    public Message toMessage(String content, QueueSession session)
            throws JMSException {

        TextMessage tm = session.createTextMessage(content);
        return tm;

    }

}


