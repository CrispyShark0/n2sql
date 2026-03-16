package com.itheima.n2sql.model.dto;

import lombok.Data;

/**
 * 统一API响应结果封装
 * 所有接口都返回这个格式，前端处理起来非常方便
 *
 * <T> 是泛型，表示 data 字段可以是任意类型
 * 比如 ApiResult<String> 的 data 就是字符串
 *     ApiResult<List<User>> 的 data 就是用户列表
 *
 * 使用示例：
 *   成功：ApiResult.success(data)
 *   失败：ApiResult.error("出错了")
 */
@Data  // Lombok注解：自动生成 getter/setter/toString 等方法，不用手写
public class ApiResult<T> {

    /** 状态码：200=成功，500=系统错误，400=参数错误 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 返回的数据（泛型，可以是任何类型） */
    private T data;

    // ========== 下面是静态工厂方法，方便快速创建 ==========

    /**
     * 成功（带数据）
     * 用法：return ApiResult.success(myData);
     */
    public static <T> ApiResult<T> success(T data) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /**
     * 成功（不带数据）
     * 用法：return ApiResult.success();
     */
    public static <T> ApiResult<T> success() {
        return success(null);
    }

    /**
     * 成功（自定义消息 + 数据）
     * 用法：return ApiResult.success("查询完成", resultList);
     */
    public static <T> ApiResult<T> success(String message, T data) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    /**
     * 失败（只有错误消息）
     * 用法：return ApiResult.error("用户名不能为空");
     */
    public static <T> ApiResult<T> error(String message) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }

    /**
     * 失败（自定义状态码 + 错误消息）
     * 用法：return ApiResult.error(404, "数据源不存在");
     */
    public static <T> ApiResult<T> error(int code, String message) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
