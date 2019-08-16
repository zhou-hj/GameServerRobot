package cn.xiaosheng996.gameserverrobot.constant;

import cn.xiaosheng996.gameserverrobot.robotManager.RobotMgr;
import cn.xiaosheng996.gameserverrobot.robotType.RobotBase;
import cn.xiaosheng996.gameserverrobot.robotType.RobotLogin;
import cn.xiaosheng996.gameserverrobot.robotType.RobotProto;

public enum RobotType {
	/**登录机器人*/
	ROBOT_TYPE_LOGIN(1) {
		
		@Override
		public void registerRobot(String account, int status) {
			RobotBase robot = new RobotLogin(account, status);
			RobotMgr.instance().register(robot);
		}
	},
	
	/**协议机器人**/
	ROBOT_TYPE_PROTO(2) {

		@Override
		public void registerRobot(String account, int status) {
			RobotBase robot = new RobotProto(account, status);
			RobotMgr.instance().register(robot);
		} 
		
	},
	
	/**世界boss机器人**/
	ROBOT_TYPE_WORLDBOSS(3) {

		@Override
		public void registerRobot(String account, int status) {
			// TODO 自动生成的方法存根
			
		}
		
	}
	;
	
	private final int type;

	RobotType(int type) {
        this.type = type;
    }
	
	public int getType() {
        return type;
    }
	
	public static RobotType valueOf(int type) {
        for (RobotType robotType : RobotType.values()) {
            if (robotType.getType() == type) {
                return robotType;
            }
        }
        return null;
    }
	
	public abstract void registerRobot(String account, int status);
}
