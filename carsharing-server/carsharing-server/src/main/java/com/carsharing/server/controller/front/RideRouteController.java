package com.carsharing.server.controller.front;

import com.carsharing.server.constant.SystemCode;
import com.carsharing.server.controller.AbstractController;
import com.carsharing.server.entity.RideDemand;
import com.carsharing.server.entity.RideRoute;
import com.carsharing.server.entity.User;
import com.carsharing.server.service.RideRouteService;
import com.carsharing.server.service.UserService;
import com.carsharing.server.util.JsonResponse;
import com.carsharing.server.util.PositionUtil;
import com.carsharing.server.util.SessionUtil;
import com.carsharing.server.vo.RideDemandVo;
import com.carsharing.server.vo.RideRouteVo;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by hucl on 2017/7/19.
 */
@RestController
@RequestMapping("/front/route/")
public class RideRouteController extends AbstractController {

    private Logger lo = Logger.getLogger(RideRouteController.class);
    @Resource
    private RideRouteService rideRouteService;
    @Resource
    private UserService userService;

    /**
     * 司机发布新的路线
     *
     * @return
     */
    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, value = "/publishNewRoute")
    public JsonResponse<Integer> publishNewRoute(RideRoute route, HttpServletRequest request) {

        JsonResponse<Integer> result = new JsonResponse<Integer>(SystemCode.FAILURE);
        User user = SessionUtil.getUser(request);
        if (user == null) {
            result.setRes(SystemCode.NO_LOGIN);
            return result;
        }
        User serUser = userService.selectByPrimaryKey(user.getUserNo());
        if (!serUser.getIsDriver()) {
            result.setRes(SystemCode.NO_PRI);
            return result;
        }
        try {
            rideRouteService.insertSelective(route);
            result.setRes(SystemCode.SUCCESS);
        } catch (Exception e) {
            lo.error("publish ride route failed!", e);
            logError(request, "publish ride route failed!", e);
        }
        return result;
    }

    /**
     * 司机触发删除一个拼车订单路线
     * 1、删除一个订单路线的前提是没有乘客加入当前路线
     * 2、不删除记录，只是把当前路线Id的state设置为删除状态
     *
     * @return
     */
    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, value = "/deleteRoute")
    public JsonResponse<String> deleteRoute(Integer routeId, HttpServletRequest request) {

        JsonResponse<String> result = new JsonResponse<String>(SystemCode.FAILURE);
        User user = SessionUtil.getUser(request);
        RideRoute route = rideRouteService.selectByPrimaryKey(routeId);
        // 是否存在id
        if (null == route) {
            result.setRes(SystemCode.OBJ_NOT_EXISTS);
            return result;
        }
        // 如果删除的不是用户自己的需求 则报错
        if (!user.getUserNo().equals(route.getUserNo())) {
            result.setRes(SystemCode.ILLEGAL_ARGUMENT);
            return result;
        }
        // state 需求单状态 0:发布中 未激活 1：发布中 已激活  2 取消发布 3:人数已满  4：执行中 5：完成
        // 需求状态>1时 不能执行
        if (route.getState() > 0) {
            result.setRes(SystemCode.PARAM_ERROR);
            return result;
        }

        try {
            route.setState(2);

            rideRouteService.updateByPrimaryKeySelective(route);
            result.setRes(SystemCode.SUCCESS);
        } catch (Exception e) {
            lo.error("delete ride route failed!", e);
            logError(request, "delete ride route failed!", e);
        }
        return result;
    }

    /**
     * 司机触发开始一个拼车订单路线
     * 1、修改当前拼车路线的状态为开始状态
     *
     * @return
     */
    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, value = "/startRoute")
    public JsonResponse<Integer> startRoute(Integer routeId, HttpServletRequest request) {

        JsonResponse<Integer> result = new JsonResponse<Integer>();
        User user = SessionUtil.getUser(request);
        RideRoute route = rideRouteService.selectByPrimaryKey(routeId);
        // 是否存在id
        if (null == route) {
            result.setRes(SystemCode.OBJ_NOT_EXISTS);
            return result;
        }
        // 如果删除的不是用户自己的需求 则报错
        if (!user.getUserNo().equals(route.getUserNo())) {
            result.setRes(SystemCode.ILLEGAL_ARGUMENT);
            return result;
        }
        // state 需求单状态 0:发布中 未激活 1：发布中 已激活  2 取消发布 3:人数已满  4：执行中 5：完成
        // 需求状态>1时 不能执行
        if (route.getState() != 1 || route.getState() != 3) {
            result.setRes(SystemCode.PARAM_ERROR);
            return result;
        }

        try {
            route.setState(4);

            rideRouteService.updateByPrimaryKeySelective(route);
            result.setRes(SystemCode.SUCCESS);
        } catch (Exception e) {
            lo.error("start ride route failed!", e);
            logError(request, "start ride route failed!", e);
        }
        return result;
    }

    /**
     * 司机完成了一次拼车路线
     * 1、要修改路线订单的状态--已完成状态
     * 2、更新司机完成拼车订单的次数，更新乘客的乘车次数
     *
     * @return
     */
    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, value = "/finishRoute")
    public JsonResponse<Integer> finishRoute(Integer routeId,HttpServletRequest request) {

        JsonResponse<Integer> result = new JsonResponse<Integer>();
        User user = SessionUtil.getUser(request);
        RideRoute route = rideRouteService.selectByPrimaryKey(routeId);
        // 是否存在id
        if (null == route) {
            result.setRes(SystemCode.OBJ_NOT_EXISTS);
            return result;
        }
        // 如果结束的不是用户自己的需求 则报错
        if (!user.getUserNo().equals(route.getUserNo())) {
            result.setRes(SystemCode.ILLEGAL_ARGUMENT);
            return result;
        }
        // state 需求单状态 0:发布中 未激活 1：发布中 已激活  2 取消发布 3:人数已满  4：执行中 5：完成
        // 需求状态不是4时 不能结束
        if (route.getState() != 4) {
            result.setRes(SystemCode.PARAM_ERROR);
            return result;
        }

        try {
            route.setState(5);
            rideRouteService.updateByPrimaryKeySelective(route);
            result.setRes(SystemCode.SUCCESS);
        } catch (Exception e) {
            lo.error(" finish ride route failed!", e);
            logError(request, "finish ride route failed!", e);
        }
        return result;
    }

    /**
     * 根据乘客的去修信息来匹配合适的司机路线
     *
     * @param demand 需求信息
     * @return
     */
    @RequestMapping(method = {RequestMethod.POST, RequestMethod.GET}, value = "/matchRideRouteByDemand")
    public JsonResponse<List<RideRouteVo>> matchRideRouteByDemand(RideDemand demand, HttpServletRequest request) {
        JsonResponse<List<RideRouteVo>> result = new JsonResponse<List<RideRouteVo>>(SystemCode.FAILURE);

        User user = SessionUtil.getUser(request);
        // 先不从服务器去判断用户是否是司机
        if(!user.getIsDriver()){
            result.setRes(SystemCode.NO_PRI);
            return result;
        }
        List<RideRouteVo> resRouteList = new ArrayList<RideRouteVo>();

        List<RideRoute> routeList = rideRouteService.getNormalStateRideRoutes();
        if (null != routeList) {
            for (RideRoute route : routeList) {
                RideRouteVo routeVo = new RideRouteVo(route);
                if (routeVo.getEndArea().equals(demand.getEndArea())) {
                    routeVo.setMatchDegree(60);
                    resRouteList.add(routeVo);

                    double distant = PositionUtil.getDistanceFromTwoPoints(route.getEndLatitude(), route.getEndLongitude(), demand.getEndLatitude(), demand.getEndLongitude());
                    if (distant < 1) {
                        routeVo.setMatchDegree(95);
                    } else if (distant < 3) {
                        routeVo.setMatchDegree(90);
                    } else if (distant < 5) {
                        routeVo.setMatchDegree(80);
                    } else if (distant < 10) {
                        routeVo.setMatchDegree(70);
                    }
                }
            }
        }
        // 排序
        if(resRouteList.size()>0) {
            Collections.sort(resRouteList, new Comparator<RideRouteVo>() {
                public int compare(RideRouteVo arg0, RideRouteVo arg1) {
                    return arg1.getMatchDegree().compareTo(arg0.getMatchDegree());
                }
            });
        }
        result.setObj(resRouteList);
        return result;
    }
}
