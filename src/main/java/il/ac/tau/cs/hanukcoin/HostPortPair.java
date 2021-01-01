package main.java.il.ac.tau.cs.hanukcoin;

import java.util.Objects;

public class HostPortPair {
    private final char port;
    private final String host;

    public HostPortPair(String host, char port) {
        this.host = host;
        this.port = port;
    }

    public char getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HostPortPair that = (HostPortPair) o;
        return port == that.port &&
                Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(port, host);
    }

    @Override
    public String toString() {
        return String.format("(%s:%d)", host, (int) port);
    }
}
