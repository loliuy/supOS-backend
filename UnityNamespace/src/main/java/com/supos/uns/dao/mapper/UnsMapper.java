package com.supos.uns.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supos.common.Constants;
import com.supos.common.vo.FieldDefineVo;
import com.supos.uns.dao.po.AlarmPo;
import com.supos.uns.dao.po.UnsPo;
import org.apache.ibatis.annotations.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Mapper
public interface UnsMapper extends BaseMapper<UnsPo> {
    @Select("select path,path_type, protocol_type, fields from " + UnsPo.TABLE_NAME + " where status=1 and (path_type=0 or path_type=2) and (data_type !=" + Constants.ALARM_RULE_TYPE+" or data_type is null)")
    List<UnsPo> listAllNamespaces();

    @Select("<script> select * from " + UnsPo.TABLE_NAME +
            " where id in " +
            "  <foreach collection=\"ids\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\"> " +
            "      #{id}" +
            "  </foreach> and path_type=0 and status=1" +
            "</script>")
    List<UnsPo> listFolders(@Param("ids") Collection<String> modelIds);

    @Select("<script> select * from " + UnsPo.TABLE_NAME +
            " where alias in " +
            "  <foreach collection=\"aliases\" item=\"alias\" index=\"index\" open=\"(\" close=\")\" separator=\",\"> " +
            "      #{alias}" +
            "  </foreach> and path_type=0 and status=1" +
            "</script>")
    List<UnsPo> listFoldersByAlias(@Param("aliases") Collection<String> aliases);

    @Select("select * from " + UnsPo.TABLE_NAME + " where path_type=2 and data_type !=" + Constants.CALCULATION_HIST_TYPE +
            " and status=1")
    List<UnsPo> listAllInstance();

    String filterPaths = " where path_type=#{pathType} " +
            "<if test=\"modelId!=null\"> and model_id = #{modelId} </if>  <if test=\"k!=null\"> and path like #{k} </if>" +
            "<choose><when test=\"dataTypes!=null and dataTypes.size() > 0\"> and data_type in <foreach collection=\"dataTypes\" item=\"dt\" index=\"index\" open=\"(\" close=\")\" separator=\",\">#{dt} </foreach></when>" +
            "<otherwise> and (data_type !=" + Constants.ALARM_RULE_TYPE + " or data_type is null)</otherwise></choose>" +
            " and status=1";

    @Select("<script> select count(1) from " + UnsPo.TABLE_NAME + filterPaths + "</script>")
    int countPaths(@Param("modelId") String modelId, @Param("k") String key, @Param("pathType") int pathType, @Param("dataTypes") Collection<Integer> dataTypes);

    @Select("<script> select path from " + UnsPo.TABLE_NAME + filterPaths + " order by path asc, create_at desc limit #{size} offset #{offset} </script>")
    ArrayList<String> listPaths(@Param("modelId") String modelId, @Param("k") String key, @Param("pathType") int pathType, @Param("dataTypes") Collection<Integer> dataTypes, @Param("offset") int offset, @Param("size") int size);

    @Select("<script> select * from " + UnsPo.TABLE_NAME + " where data_type=#{dataType} and path_type=2 <if test=\"k!=null\"> and path like #{k} </if> " +
            "and status=1 order by create_at desc limit #{size} offset #{offset} </script>")
    ArrayList<UnsPo> listByDataType(@Param("k") String key, @Param("dataType") int dataType, @Param("offset") int offset, @Param("size") int size);

    @Select("<script> select count(*) from " + UnsPo.TABLE_NAME + " where data_type=#{dataType} and path_type=2 <if test=\"k!=null\"> and path like #{k} </if> " +
            "and status=1" +
            "</script>")
    int countByDataType(@Param("k") String key, @Param("dataType") int dataType);

    @Select("<script> select id from " + UnsPo.TABLE_NAME +
            " where id in " +
            "  <foreach collection=\"ids\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\"> " +
            "      #{id}" +
            "  </foreach> and path_type=2 and status=1" +
            "</script>")
    Set<String> listInstanceIds(@Param("ids") Collection<String> instanceIds);

    @Select("<script> select * from " + UnsPo.TABLE_NAME +
            " where id in " +
            "  <foreach collection=\"ids\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\"> " +
            "      #{id}" +
            "  </foreach> and path_type=2 and status=1" +
            "</script>")
    List<UnsPo> listInstanceByIds(@Param("ids") Collection<String> instanceIds);

    @Select("<script> select * from " + UnsPo.TABLE_NAME +
            " where alias in " +
            "  <foreach collection=\"aliases\" item=\"alias\" index=\"index\" open=\"(\" close=\")\" separator=\",\"> " +
            "      #{alias}" +
            "  </foreach> and path_type=2 and status=1" +
            "</script>")
    List<UnsPo> listInstanceIdsByAlias(@Param("aliases") Collection<String> aliases);

    @Insert("<script> INSERT INTO " + UnsPo.TABLE_NAME + "(id,path,path_type,data_type,data_src_id,data_path,with_flags,fields,description,protocol,alias,protocol_type,refers,expression,table_name,number_fields,model_id,extend) VALUES" +
            " <foreach collection=\"beans\" item=\"un\" separator=\",\">" +
            "     (#{un.id},#{un.path},#{un.pathType},#{un.dataType},#{un.dataSrcId},#{un.dataPath},#{un.withFlags},#{un.fields},#{un.description},#{un.protocol},#{un.alias},#{un.protocolType},#{un.refers},#{un.expression},#{un.tableName},#{un.numberFields},#{un.modelId},#{un.extend})" +
            " </foreach> ON CONFLICT (id) do update set refers=EXCLUDED.refers, expression=EXCLUDED.expression" +
            " ,data_path=EXCLUDED.data_path ,description=EXCLUDED.description,protocol=EXCLUDED.protocol " +
            "</script>")
    int saveOrIgnoreBatch(@Param("beans") Collection<UnsPo> beans);

    @Update("update " + UnsPo.TABLE_NAME + " set fields=#{fs},data_src_id=2,table_name='supos." + AlarmPo.TABLE_NAME +
            "', update_at=now(),with_flags=" + (Constants.UNS_FLAG_WITH_SAVE2DB | Constants.UNS_FLAG_RETAIN_TABLE_WHEN_DEL_INSTANCE) +
            "  where data_type=" + Constants.ALARM_RULE_TYPE)
    int fixAlarmFields(@Param("fs") String fields);

    @Select("select * from " + UnsPo.TABLE_NAME + " where path_type=2 limit 1 offset 0")
    UnsPo getFirstInstance();

    @Select("select * from " + UnsPo.TABLE_NAME + " where alias=#{alias}")
    UnsPo getByAlias(@Param("alias") String alias);

    @Update("update " + UnsPo.TABLE_NAME + " set fields=#{fields}, number_fields=#{numberCount}, update_at=now() where id=#{id}")
    int updateModelFieldsById(@Param("id") String id, @Param("fields") String fields, @Param("numberCount") int numberCount);

    @Update("<script> update " + UnsPo.TABLE_NAME + " set fields=#{fields}, number_fields=#{numberCount}, update_at=now() where id in " +
            "  <foreach collection=\"ids\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\"> " +
            "      #{id}" +
            "  </foreach>" +
            "</script>")
    int updateInstanceFieldsByIds(@Param("ids") List<String> ids, @Param("fields") String fields, @Param("numberCount") int numberCount);

    @Update("update " + UnsPo.TABLE_NAME + " set description=#{description}, update_at=now() where alias=#{alias}")
    int updateDescByAlias(@Param("alias") String alias, @Param("description") String description);

    @Select("<script> select * from " + UnsPo.TABLE_NAME +
            " where id in " +
            "  <foreach collection=\"ids\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\"> " +
            "      #{id}" +
            "  </foreach> and status=1" +
            "</script>")
    List<UnsPo> listInstancesById(@Param("ids") Collection<String> ids);

    @Select("select * from " + UnsPo.TABLE_NAME + " where model_id=#{modelId} and path_type=2 and status=1")
    List<UnsPo> listInstancesByModel(@Param("modelId") String modelId);


    @Select("SELECT column_name as name, udt_name as type FROM information_schema.columns WHERE table_name = #{t} and table_schema = 'supos'")
    List<FieldDefineVo> describeTableFieldInfo(@Param("t") String table);


    @Select("<script> select count(1) from " + UnsPo.TABLE_NAME + " where path_type=2 and data_type=" + Constants.TIME_SEQUENCE_TYPE +
            " <if test=\"k!=null\"> and path like #{k} </if>" +
            " <if test=\"nfc!=null\"> and number_fields >=  #{nfc} </if>" +
            " and status=1 </script>")
    int countNotCalcSeqInstance(@Param("k") String key, @Param("nfc") Integer minNumFields);

    @Select("<script> select path, fields from " + UnsPo.TABLE_NAME + " where path_type=2 and data_type=" + Constants.TIME_SEQUENCE_TYPE +
            "<if test=\"k!=null\"> and path like #{k} </if> " +
            " <if test=\"nfc!=null\"> and number_fields >=  #{nfc} </if>" +
            "and status=1 order by create_at desc limit #{size} offset #{offset} </script>")
    ArrayList<UnsPo> listNotCalcSeqInstance(@Param("k") String key, @Param("nfc") Integer minNumFields, @Param("offset") int offset, @Param("size") int size);

    @Select("<script> select count(1) from " + UnsPo.TABLE_NAME + " where path_type=2 and data_type in (" + Constants.TIME_SEQUENCE_TYPE + "," + Constants.CALCULATION_REAL_TYPE + ")" +
            " <if test=\"k!=null\"> and path like #{k} </if> and number_fields > 0 and status=1 </script>")
    int countTimeSeriesInstance(@Param("k") String key);

    @Select("<script> select count(1) from " + UnsPo.TABLE_NAME + " where path_type=2  and data_type = " + Constants.ALARM_RULE_TYPE +
            " <if test=\"k!=null\"> and (data_path like #{k} or description like #{k}) </if> and status=1 </script>")
    int countAlarmRules(@Param("k") String key);


    @Select("<script> select path, fields from " + UnsPo.TABLE_NAME + " where path_type=2 and data_type in (" + Constants.TIME_SEQUENCE_TYPE + "," + Constants.CALCULATION_REAL_TYPE + ")" +
            "<if test=\"k!=null\"> and path like #{k} </if> and number_fields > 0 " +
            "and status=1 order by create_at desc limit #{size} offset #{offset} </script>")
    ArrayList<UnsPo> listTimeSeriesInstance(@Param("k") String key, @Param("offset") int offset, @Param("size") int size);

    @Select("<script> select * from " + UnsPo.TABLE_NAME + " where path_type=2 and data_type = " + Constants.ALARM_RULE_TYPE +
            "<if test=\"k!=null\"> and (data_path like #{k} or description like #{k}) </if> " +
            "and status=1 order by create_at desc limit #{size} offset #{offset} </script>")
    ArrayList<UnsPo> listAlarmRules(@Param("k") String key, @Param("offset") int offset, @Param("size") int size);

    @UpdateProvider(type = UnsRefUpdateProvider.class, method = "updateRefUns")
    void updateRefUns(@Param("id") String id, @Param("topics") Collection<String> calcTopic);

    @UpdateProvider(type = UnsRefUpdateProvider.class, method = "removeRefUns")
    void removeRefUns(@Param("id") String id, @Param("topics") Collection<String> calcTopic);

    @Select("<script> SELECT n.* FROM " + UnsPo.TABLE_NAME + " n" +
            " where (n.path_type=0 or n.path_type=2) and (n.data_type !=" + Constants.ALARM_RULE_TYPE+" or n.data_type is null or n.data_type=0)" +
            " and n.model_id is not null" +
            " <if test=\"name!=null and name!='' \"> and lower(n.path) like '${'%' + name.toLowerCase() + '%'}'  </if> " +
            " GROUP BY n.id order by n.path_type asc</script>")
    List<UnsPo> listInTemplate(@Param("name") String name);

    class UnsRefUpdateProvider {
        public static String updateRefUns(@Param("id") String id, @Param("topics") Collection<String> calcTopic) {
            StringBuilder sql = new StringBuilder(128 + calcTopic.size());
            sql.append("UPDATE ").append(UnsPo.TABLE_NAME).append(" SET ref_uns = ");
            for (int i = 0, sz = calcTopic.size(); i < sz; i++) {
                sql.append("jsonb_set(");
            }
            sql.append("case when ref_uns is null then '{}' else ref_uns end");
            for (String topic : calcTopic) {
                sql.append(",'{\"").append(topic).append("\"}','1')");
            }
            sql.append(" where id=#{id}");
            return sql.toString();
        }

        public static String removeRefUns(@Param("id") String id, @Param("topics") Collection<String> calcTopic) {
            StringBuilder sql = new StringBuilder(128 + calcTopic.size());
            sql.append("UPDATE ").append(UnsPo.TABLE_NAME).append(" SET ref_uns = ");
            for (int i = 0, sz = calcTopic.size(); i < sz; i++) {
                sql.append("jsonb_set_lax(");
            }
            sql.append("ref_uns");
            for (String topic : calcTopic) {
                sql.append(",'{\"").append(topic).append("\"}',null,true,'delete_key')");
            }
            sql.append(" where id=#{id}");
            return sql.toString();
        }

    }


}
