package com.carsharing.server.controller.front;

import com.carsharing.server.constant.SystemCode;
import com.carsharing.server.controller.AbstractController;
import com.carsharing.server.entity.Driver;
import com.carsharing.server.service.DriverService;
import com.carsharing.server.service.UserService;
import com.carsharing.server.util.JsonResponse;
import com.carsharing.server.util.SessionUtil;
import org.apache.log4j.Logger;
import org.omg.CORBA.Request;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Created by hucl on 2017/7/12.
 * 司机信息设置
 */
@RestController
@RequestMapping("/front/driver")
public class DriverController extends AbstractController {
    private Logger lo = Logger.getLogger(DriverController.class);
    @Resource
    private DriverService driverService;

    /**
     * 1.员工首次注册司机时增加司机的信息
     * 2.更新司机信息
     * @return
     */
    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST},value = "/regToDriver")
    public JsonResponse<String> regToDriver(Driver driver,
                                            HttpServletRequest request){

        JsonResponse<String> result = new JsonResponse<String>( SystemCode.FAILURE);
        try {
            Driver _driver = driverService.selectByPrimaryKey(driver.getUserNo());
            if (_driver == null)
                driverService.insertService(driver);
            else driverService.updateService(driver);
            result.setRes(SystemCode.SUCCESS);
        }
        catch (Exception e)
        {
            lo.error("司机信息操作失败", e);
            logError(request, "[司机信息操作失败]", e);
        }
        return result;
    }

    /**
     * 获取司机信息
     */
    @RequestMapping(method = {RequestMethod.POST, RequestMethod.GET}, value = "/getDriverByNo")
    public JsonResponse<Driver> getDriverByNo(String userNo,
                                          HttpServletRequest request) {
        JsonResponse<Driver> result = new JsonResponse<Driver>(SystemCode.FAILURE);
        lo.info("getDriverByNo...");
        List<Driver> driverList = driverService.selectByUserNo(userNo);
        if (null == driverList||driverList.size()<1) {
            result.setRes(SystemCode.OBJ_NOT_EXISTS);
            return result;
        } else {
            Driver driver =driverList.get(0);
            if (driver != null) {
                result.setRes(SystemCode.SUCCESS);
                result.setObj(driver);
            }
        }
        return result;
    }
}
