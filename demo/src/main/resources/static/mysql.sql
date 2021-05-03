CREATE TABLE t_user (
id BIGINT NOT NULL COMMENT '用户ID,手机号码',
nickname VARCHAR(255)  NOT NULL COMMENT '昵称',
password VARCHAR(32)  DEFAULT NULL COMMENT 'MDS+salt加密',
slat VARCHAR ( 10 ) DEFAULT NULL COMMENT '盐',
head VARCHAR ( 255 ) DEFAULT NULL COMMENT '头像',
register_date datetime DEFAULT NULL COMMENT '注册时间',
last_login_date datetime DEFAULT NULL COMMENT '最后一次登录时间',
login_count INT ( 10 ) DEFAULT '0' COMMENT '登录次数'
)

