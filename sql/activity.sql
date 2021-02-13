--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(128) NOT NULL COMMENT '用户名',
  `password` varchar(128) NOT NULL DEFAULT '123456' COMMENT '密码',
  `token` varchar(128) NOT NULL COMMENT 'token',
  `wx_token` varchar(128) NOT NULL COMMENT '微信session_key',
  `wx_nick` varchar(256) NOT NULL COMMENT '微信昵称',
  `nick` varchar(256) NOT NULL COMMENT '昵称',
  `sex` tinyint(2) NOT NULL DEFAULT 1 COMMENT '性别:1男,2女',
  `phone` varchar(11) DEFAULT NULL COMMENT '手机号码',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `ip` varchar(128) DEFAULT 0 COMMENT 'ip地址',
  `activities` text COMMENT '参加的活动列表',
  `groups` text COMMENT '加入的群组',
  `create_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_at` datetime DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username_uindex` (`username`) USING BTREE,
  KEY `token_index` (`token`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户表';

--
-- Table structure for table `group`
--

DROP TABLE IF EXISTS `group`;
CREATE TABLE `group` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '群组ID',
  `level` tinyint(2) unsigned NOT NULL DEFAULT 1 COMMENT '群组等级',
  `name` varchar(128) NOT NULL COMMENT '群组名称',
  `logo` varchar(64) COMMENT '群组LOGO',
  `members` text COMMENT '成员列表',
  `pending` text COMMENT '待审批队列',
  `notice` text COMMENT '群组公告',
  `addr` varchar(256) DEFAULT NULL COMMENT '群组总部地址',
  `activities` text COMMENT '群组组织的活动列表',
  `create_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_at` datetime DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `level_index` (`level`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='群组表';

--
-- Table structure for table `activity`
--

DROP TABLE IF EXISTS `activity`;
CREATE TABLE `activity` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '活动ID',
  `planner` int(11) unsigned NOT NULL COMMENT '组织者ID',
  `group_id` int(11) unsigned NOT NULL DEFAULT 0 COMMENT '群组ID',
  `kind` tinyint(4) unsigned NOT NULL DEFAULT 1 COMMENT '活动分类:1羽毛球,2篮球,3足球,4聚餐...',
  `type` tinyint(2) unsigned NOT NULL DEFAULT 1 COMMENT '活动类型:1全局保护,2全局公开,3群组',
  `quota` int(11) unsigned NOT NULL DEFAULT 1 COMMENT '名额',
  `title` varchar(256) NOT NULL COMMENT '活动标题',
  `remark` text COMMENT '活动描述',
  `status` tinyint(2) unsigned NOT NULL DEFAULT 1 COMMENT '活动状态:1进行中,2正常结算完成,3手动终止',
  `fee_type` tinyint(2) unsigned NOT NULL DEFAULT 1 COMMENT '结算方式:1免费,2活动前,3活动后男女平均,4活动后男固定|女平摊,5活动后男平摊|女固定',
  `fee_male` int(11) unsigned NOT NULL DEFAULT 0 COMMENT '男费用,单位:分',
  `fee_female` int(11) unsigned NOT NULL DEFAULT 0 COMMENT '女费用,单位:分',
  `queue` text COMMENT '报名列表',
  `queue_sex` text COMMENT '报名队列中性别',
  `addr` varchar(256) COMMENT '活动地址',
  `ahead` tinyint(4) NOT NULL COMMENT '可提前取消时间(小时)',
  `begin_at` datetime NOT NULL COMMENT '开始时间',
  `end_at` datetime NOT NULL COMMENT '结束时间',
  `create_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_at` datetime DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `kind_status_index` (`kind`, `status`) USING BTREE,
  KEY `type_status_index` (`type`, `status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='活动表';
