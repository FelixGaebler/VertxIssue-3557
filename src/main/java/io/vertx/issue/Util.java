package io.vertx.issue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Util {

    @SuppressWarnings("all")
    public static <T> T serialize(Class<T> clazz, Map<String, Object> map, T object) {

        try {

            for(Field field : clazz.getDeclaredFields()) {
                String fieldName = field.getName();

                if(map.containsKey(fieldName)) {
                    Object value = map.get(fieldName);
                    field.setAccessible(true);

                    if(field.getType() == List.class) {
                        List<Object> embedded = (List) value;

                        ParameterizedType listType = (ParameterizedType) field.getGenericType();
                        Class listClass = (Class) listType.getActualTypeArguments()[0];

                        if(!listClass.getPackageName().equals("java.lang")) {
                            List<Object> list = embedded.stream()
                                    .map(obj -> {
                                        try {
                                            return serialize(listClass, ((HashMap) obj), listClass.getDeclaredConstructor().newInstance());
                                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                                            e.printStackTrace();
                                            return null;
                                        }
                                    })
                                    .collect(Collectors.toList());

                            field.set(object, list);
                            continue;
                        }
                    }

                    field.set(object, value);
                }
            }

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return object;
    }

}
