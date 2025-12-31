package tools.vlab.kberry.server.commands;

public interface CommandTopic {

    String[] getTopicPath();

    default String getTopic() {
        return String.join("/", this.getTopicPath());
    }

    default String getIdPath() {
        return String.join(".", getTopicPath());
    }
}
