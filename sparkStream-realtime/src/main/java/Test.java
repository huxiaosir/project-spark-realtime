import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @Author Joisen
 * @Date 2022/12/28 10:19
 * @Version 1.0
 */
public class Test {

    public static void main(String[] args) {
//        int[] height = new int[]{0,1,0,2,1,0,1,3,2,1,2,1};
//        System.out.println(trap(height));
//        int[] tmp = new int[10];
//        System.out.println(Arrays.toString(tmp));

//        String[] strs = new String[]{"eat", "tea", "tan", "ate", "nat", "bat"};
//        List<List<String>> res = groupAnagrams(strs);
//        System.out.println(res);
        System.out.println(areNumbersAscending("4 5 11 26 7"));

    }
    public static boolean areNumbersAscending(String s) {
        String[] arr = s.split(" ");
        int pre = -1;
        for (String str : arr) {
            if( (str.charAt(0) - '0') >= 0 && (str.charAt(0) - '0' <= 9) ){
                if( Integer.parseInt(str)  <= pre ) return false;
                pre = Integer.parseInt(str);
            }
        }
        return true;
    }
    public static int trap(int[] height){
        int res = 0;
        int len = height.length;
        int left = 0, right = len - 1;
        int flag = 1, max = 0, sum = 0;
        for (int h : height) {
            sum += h;
            if(h > max) max = h;
        }
        while(left <= right && flag <= max){
            while(left <= right && height[left] < flag) left++;
            while(left <= right && height[right] < flag) right--;
            res += right - left + 1;
            flag ++;
        }
        return res - sum;
    }


    public static List<List<String>> groupAnagrams(String[] strs){
        HashMap<String, ArrayList<String>> map = new HashMap<>();
        for(String s: strs){
            char[] arr = s.toCharArray();
            Arrays.sort(arr);
            String key = String.valueOf(arr);
            if(!map.containsKey(key)) map.put(key, new ArrayList<>());
            map.get(key).add(s);
        }
        return new ArrayList<>(map.values());
    }

}
