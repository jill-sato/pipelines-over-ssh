package com.illumio.build.sshpipelines;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.LogRecord;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Slave;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.OfflineCause;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.SlaveComputer;
import hudson.util.Futures;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * External slave.
 */
public class ExternalSlave extends Slave {

    private static final EnvVars EMPTY_ENV_VARS = new EnvVars();

    private String host;

    /**
     * External slave constructor.
     *
     * @param name slave name
     * @param host slave host name
     * @throws Descriptor.FormException if a form exception is thrown by the parent constructor
     * @throws IOException              if an IO exception if thrown by the parent constructor
     */
    @DataBoundConstructor
    public ExternalSlave(String name, String host) throws Descriptor.FormException, IOException {
        super(name, "/dev/null", new NoOpLauncher());
        this.host = Util.fixEmptyAndTrim(host);
        setNodeDescription("External slave");
        setMode(Mode.EXCLUSIVE);
        setNumExecutors(1);
        setRetentionStrategy(RetentionStrategy.Always.INSTANCE);
    }

    @Override
    public Computer createComputer() {
        return new ComputerImpl();
    }

    /**
     * Set the hostname.
     *
     * @param value hostname value
     */
    @DataBoundSetter
    public void setHost(String value) {
        this.host = Util.fixEmptyAndTrim(value);
    }

    /**
     * Descriptor class.
     */
    @Extension
    @Symbol({"externalAgent"})
    public static final class DescriptorImpl extends SlaveDescriptor {

        public String getDisplayName() {
            return "External Agent";
        }
    }

    /**
     * NO-OP launcher.
     */
    private static final class NoOpLauncher extends ComputerLauncher {

        @Override
        public void launch(SlaveComputer computer, TaskListener listener) {
        }
    }

    /**
     * {@link SlaveComputer} implementation.SlaveComputer
     */
    private final class ComputerImpl extends SlaveComputer {

        private final AtomicBoolean offline = new AtomicBoolean(true);

        ComputerImpl() {
            super(ExternalSlave.this);
        }

        @Override
        public Map<String, String> getThreadDump() {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, Object> getMonitorData() {
            return Collections.emptyMap();
        }

        @Override
        public EnvVars getEnvironment() {
            return EMPTY_ENV_VARS;
        }

        @Override
        public EnvVars buildEnvironment(TaskListener listener) {
            return EMPTY_ENV_VARS;
        }

        @Override
        public Map<Object, Object> getSystemProperties() {
            return Collections.emptyMap();
        }

        @Override
        public List<LogRecord> getLogRecords() {
            return Collections.emptyList();
        }

        @Override
        public Charset getDefaultCharset() {
            return StandardCharsets.UTF_8;
        }

        @Override
        public boolean isOffline() {
            return offline.get();
        }

        @Override
        public void setChannel(Channel channel, OutputStream launchLog, Channel.Listener listener) {
            // do nothing, this computer does not have a channel
        }

        @Override
        public boolean isConnecting() {
            return false;
        }

        @Override
        protected Future<?> _connect(boolean forceReconnect) {
            offline.compareAndSet(true, false);
            return Futures.precomputed(null);
        }

        @Override
        public Future<?> disconnect(OfflineCause cause) {
            offline.compareAndSet(false, true);
            setTemporarilyOffline(false, cause);
            return Futures.precomputed(null);
        }
    }
}
