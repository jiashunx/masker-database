
CREATE TABLE test.tb_user (
                              user_id          VARCHAR (32),
                              user_name        VARCHAR (256) NOT NULL,
                              user_age         INT            NOT NULL,
                              create_time      TIMESTAMP      NOT NULL,
                              last_modify_time TIMESTAMP      NOT NULL,
                              PRIMARY KEY (
                                           user_id
                                  )
);
