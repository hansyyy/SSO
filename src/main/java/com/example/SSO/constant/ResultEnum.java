package com.example.SSO.constant;

/**
 * @Author HanSiyue
 * @Date 2019/9/18 下午3:30
 */
public enum ResultEnum {
    /**
     * 状态码记录
     * 0----成功
     * 1----失败
     * 2----不存在
     * 3----已存在
     * 4----参数为空
     */

    SUCCESS(0,"成功"),
    ERROR(1,"失败"),
    NOTEXIST(2,"不存在"),
    ISEXIST(3,"已存在"),
    ISNULL(4,"参数为空"),
    ;
    private Integer status;
    private String msg;

    ResultEnum(int status,String msg){
        this.status=status;
        this.msg=msg;
    }

    public Integer getStatus() {
        return status;
    }

    public String getMsg() {
        return msg;
    }
}
