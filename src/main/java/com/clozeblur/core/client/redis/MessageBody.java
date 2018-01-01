package com.clozeblur.core.client.redis;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * @author clozeblur
 * <html>获取实时最新配置的消息体</html>
 */
public class MessageBody {

    private static final long serialVersionUID = -3894764726332439527L;

    public MessageBody(Map<String, String> source, String branch, String env, String filename) {
        this.branch = branch;
        this.env = env;
        this.filename = filename;
        this.content = source;
        this.createTime = new Date();
        this.uniqueId = UUID.randomUUID().toString();
    }

    private String uniqueId;
    private String branch;
    private String env;
    private String filename;
    private Map<String, String> content;
    private Date createTime;
    private Date finishTime;

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Map<String, String> getContent() {
        return content;
    }

    public void setContent(Map<String, String> content) {
        this.content = content;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

    @Override
    public String toString() {
        return "MessageBody{" +
                "uniqueId='" + uniqueId + '\'' +
                ", branch='" + branch + '\'' +
                ", env='" + env + '\'' +
                ", filename='" + filename + '\'' +
                ", content=" + content +
                ", createTime=" + createTime +
                ", finishTime=" + finishTime +
                '}';
    }
}
