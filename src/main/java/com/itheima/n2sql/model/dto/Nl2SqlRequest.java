package com.itheima.n2sql.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * NL2SQL 请求体
 *
 * 前端调用"自然语言转SQL"接口时需要传的数据。
 * 很简单，只需要两样东西：你要查哪个数据库 + 你想问什么问题。
 */
@Data
public class Nl2SqlRequest {

    /** 数据源ID（指定在哪个数据库上执行） */
    @NotBlank(message = "数据源ID不能为空")
    private String dataSourceId;

    /** 用户的自然语言问题（如 "查询所有年龄大于25的用户"） */
    @NotBlank(message = "问题不能为空")
    private String question;
}
