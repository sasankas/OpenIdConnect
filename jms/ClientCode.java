  private void putMessage(XmlMessage msg,
                            String[] errorProps,
                            Module destModule,
                            String destQueue)
        throws xxxxxxxxException {

        IQueueSender sender = null;

        try {
            sender = this.m_queueHandler.newSender(destModule, destQueue);
            // Extract header
            MsgHeader header = new MsgHeader(msg.getHeader());
            header.setRecipientEnterpriseId(Application.getInstance().getContext().getName());
            header.setRecipientSystemId(destModule.getName());

            // Extract footer and set the sending/routing time to destination queue.
            MsgFooter footer = new MsgFooter(msg.getFooter());
            footer.setSendingTime(new Date());

            int priority = getMessagePriority(msg,destModule,destQueue);

            String content = XmlMessageOutputter.getDefaultOutputter(
                                                header.getDtdType()).toString(msg);
            putMessage(content, errorProps, sender, priority, false);

        } finally {
            QueueUtils.closeQuietly(sender);
        }
    }
 
 
 private void putMessage(String content,
                            String[] errorProps,
                            IQueueSender sender,
                            int priority,
                            boolean quiet)
        throws xxxxxxxxException {

        try {
            IQueueMessage message = this.m_queueHandler.newQueueMessage(content);
            if (errorProps != null) {
                message.getMessage().setStringProperty(Constants.JMS_PROP_ERROR_CODE,
                                                       errorProps[0]);
                message.getMessage().setStringProperty(Constants.JMS_PROP_ERROR_DESC,
                                                       errorProps[1]);
                message.getMessage().setStringProperty(Constants.JMS_PROP_MSG_TYPE,
                                                       errorProps[2]);
                message.getMessage().setStringProperty(Constants.JMS_PROP_COMPONENT_ID,
                                                       errorProps[3]);
                message.getMessage().setStringProperty(Constants.JMS_PROP_SENDER_SYSTEM_ID,
                        							   errorProps[4]);
                message.getMessage().setStringProperty(Constants.JMS_PROP_ERROR_TIMES,
						   								errorProps[5]);
            }
            if(priority == -1)
                sender.send(message);
            else
                sender.send(message,-1,priority,-1);

        } catch (xxxxxxxxException e) {
            if(!quiet) {
                log.error("Exception writing message",e);
                throw e;
            }

        } catch (JMSException e) {
            if(!quiet) {
                log.error("Exception writing message",e);
                throw new xxxxxxxxStandardException(e);
            }
        }

    }
	
	
	
    /**
     * Retrieves a message from input queue of the queue handler.
     * @param qHandler the queue handler
     * @return the queue message
     * @throws JMSException indicating queue read failure
     * @throws xxxxxxxxException indicating failure
     */
    private IQueueMessage read(QueueHandler qHandler)
            throws JMSException, xxxxxxxxException {

        // Reads message from queue receiver
        IQueueReceiver qRcvr = null;

        try {

            qRcvr = qHandler
                            .newReceiver(m_options.getModule(), m_options.getInputQName());
            return qRcvr.receive(m_options.getWaitInterval());

        } finally {

            QueueUtils.closeQuietly(qRcvr);
        }

    }

    /**
     * Releases following resources and resets to null.
     * @param conn - the database connection
     * @param qHandler - the queue handler
     */
    private void releaseResources(Connection conn,
                                  QueueHandler qHandler) {

        try {

            if (conn != null) {
                DbUtils.closeQuietly(conn);
            }
            if (qHandler != null) {
                qHandler.close();
            }

        } finally {

            conn = null;
            qHandler = null;

        }
    }

    /**
     * Finds out the specific message processor handler corresponding to
     * message type and process the message.
     * @param con - the db connection
     * @param qHandler - the queue handler
     * @param xmlMsg - the message
     * @throws xxxxxxxxException indicating failure to process
     */
    private void processMessage(Connection con,
                                QueueHandler qHandler,
                                XmlMessage xmlMsg)
            throws xxxxxxxxException {

        try {

            // Prepares the corresponding message processor class
            BaseMessageProcessor msgProcessor = (BaseMessageProcessor) MsgProcessorFactory
                    .getInstance().makeMsgProcessor(con,
                                                    m_options.getModule(),
                                                    qHandler,
                                                    null,
                                                    m_options.getOutputQName(),
                                                    xmlMsg);

            msgProcessor.processMessage();

        } catch (xxxxxxxxException e) {
            throw e;
        } catch (Exception ex) {
            throw new xxxxxxxxStandardException(ex);
        }
    }

    /**
     * Parse the message -read and build the <code>XmlMessage</code>
     * after parsing the message.
     * @param   message - the queue message, never null
     * @return  the parsed XmlMessage
     */
    XmlMessage parseMessage(IQueueMessage message,boolean validating)
            throws xxxxxxxxException {
    	XmlParserBase parser = XmlParser.getDefaultInstance().setValidating(validating);

        XmlMessage msg = (XmlMessage) parser.parse(message.getContent());
        log.info("System id :-" + parser.getSystemId());

        return msg;

    }

    /**
     * Put the parsed Xml message to error queue.
     * @param qHandler the queue handler
     * @param xmlMessage the message
     * @param errorCode the error code
     * @param errorDescription the error description
     * @throws JMSException indicating jms error
     * @throws xxxxxxxxException indicating internal error
     */
    private void writeError(QueueHandler qHandler,
                            XmlMessage xmlMessage,
                            String errorCode,
                            String errorDescription)
            throws JMSException, xxxxxxxxException {

        // Modified header
        MsgHeader header = new MsgHeader(xmlMessage.getHeader());
        header.setErrorEnterpriseId(Application.getInstance().getContext().getName());
        header.setErrorSystemId(m_options.getModule().getName());
        header.increaseErrorTimes();
        header.setErrorCode(errorCode);
        header.setErrorStatus(1);

        String content = XmlMessageOutputter.getDefaultOutputter(
                                            header.getDtdType()).toString(xmlMessage);

        IQueueMessage message = qHandler.newQueueMessage(content);
        message.getMessage().setStringProperty(Constants.JMS_PROP_ERROR_CODE, errorCode);
        message.getMessage().setStringProperty(Constants.JMS_PROP_ERROR_DESC, errorDescription);
        message.getMessage().setStringProperty(Constants.JMS_PROP_MSG_TYPE, header.getMessageType());
        message.getMessage().setStringProperty(Constants.JMS_PROP_COMPONENT_ID, header.getErrorSystemId());
        message.getMessage().setStringProperty(Constants.JMS_PROP_SENDER_SYSTEM_ID, header.getSenderSystemId());
        message.getMessage().setStringProperty(Constants.JMS_PROP_ERROR_TIMES, String.valueOf(header.getErrorTimes()));


        putMessage(qHandler, message);

    }

    /**
     * Put the input message to error queue.
     * @param qHandler the queue handler
     * @param qMessage the message
     * @param errorCode the error code
     * @param errorDesc the error description
     * @throws JMSException indicating jms error
     * @throws xxxxxxxxException indicating internal error
     */
    private void writeError(QueueHandler qHandler,
                            IQueueMessage qMessage,
                            String errorCode,
                            String errorDesc)
            throws JMSException, xxxxxxxxException {
    	//we should clear properties of the JMS message before setting any JMS property.
    	qMessage.getMessage().clearProperties();
        qMessage.getMessage().setStringProperty(Constants.JMS_PROP_ERROR_CODE, errorCode);
        qMessage.getMessage().setStringProperty(Constants.JMS_PROP_ERROR_DESC, errorDesc);
        putMessage(qHandler, qMessage);
    }

    /**
     * Checks whether it is required to continue the process ongoing.
     * It depends on the error in Status or any interruption
     * @return true/false
     */
    private boolean interruptedProcess() {

        if (MonitorServiceStatus.getInstance().hasError()
            || Thread.currentThread().isInterrupted()) {
            log.debug("    Process interrupted.");
            return true;
        }

        return false;
    }

    /**
     * Put the message document in destination queue.
     * @param qHandler query handler
     * @param qMessage jms message
     * @throws xxxxxxxxException
     */
    private void putMessage(QueueHandler qHandler,
                            IQueueMessage qMessage)
            throws JMSException, xxxxxxxxException {

        IQueueSender qSndr = null;

        try {

            qSndr = qHandler.newSender(m_options.getModule(), m_options.getErrorQName());
            qSndr.send(qMessage);

        } finally {

            QueueUtils.closeQuietly(qSndr);
        }
    }
