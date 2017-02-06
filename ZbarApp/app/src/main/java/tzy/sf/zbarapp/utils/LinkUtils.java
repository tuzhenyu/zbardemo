package tzy.sf.zbarapp.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkUtils {
    public static final String REGEX_URL = "((?:(?:ht|f)tps?://)*(?:[a-zA-Z0-9-]+\\.)+(?:com|net|php|jsp|asp|org|info|mil|gov|edu|name|xxx|[a-z]{2}){1}(?::[0-9]+)?(?:(/|\\?)[a-zA-Z0-9\\^\\.\\{\\}\\(\\)\\[\\]_\\?,'/\\\\+&%\\$:#=~-]*)*)";
   //  public static final String REGEX_URL = "((?:(?:ht|f)tps?://)*(((?:[a-zA-Z0-9-]+\\.)+(?:com|net|org|info|mil|gov|edu|name|xxx|[a-z]{2}){1})|(((2[0-4]\\d|25[0-5]|[01]?\\\\d\\\\d?)\\\\.){3}(2[0-4]\\\\d|25[0-5]|[01]?\\\\d\\\\d?)))(?::[0-9]+)?(?:(/|\\\\?)[a-zA-Z0-9\\\\^\\\\.\\\\{\\\\}\\\\(\\\\)\\\\[\\\\]_\\\\?,'/\\\\\\\\+&%\\\\$:#@=~-]*)*)";
    public static final String REGEX_IP = "((?:(?:ht|f)tps?://)*(?:\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})(?::[0-9]+)?(?![a-z])(?:/[a-zA-Z0-9\\^\\.\\{\\}\\(\\)\\[\\]_\\?,'/\\\\+&%\\$:#=~-]*)*)";
    public static final String REGEX_PHONE = "(^|(?<=\\D))((\\+?)(([0-9]{2,6}-[0-9]{3,9})((-[0-9]{2,9})?)|[0-9]{7,20}))($|(?=\\D))";
    public static final String REGEX_EMAIL = "[_a-zA-Z0-9-]{1,30}(\\.[_a-zA-Z0-9-]+){0,30}@[a-zA-Z0-9-]{1,30}(\\.[a-zA-Z0-9-]+){0,30}(\\.[a-zA-Z]{2,4})";
    public static final String REGEX_TEST = "kdtest://(\\w+).test";
    public static final String PREFIX_EMAIL = "mailto:";
    public static final String PREFIX_PHONE = "tel:";

//	public static boolean isUrl(String text) {
//		Pattern p = Pattern.compile(REGEX_URL, Pattern.CASE_INSENSITIVE);
//		Matcher m = p.matcher(text);
//		if (m.find()) {
//			String str = m.group(0);
//			return str.equals(text);
//		}
//
//		return false;
//	}

	public static boolean isIp(String text) {
		Pattern p = Pattern.compile(REGEX_IP, Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(text);
		if (m.find()) {
			String str = m.group(0);
			return str.equals(text);
		}

		return false;
	}

    public static boolean isPhone(String text) {
        if (text.startsWith(PREFIX_PHONE)) {
            return true;
        }

        Pattern p = Pattern.compile(REGEX_PHONE, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            String str = m.group(0);
            return str.equals(text);
        }

        return false;
    }

    public static boolean isEmail(String text) {
        if (text.startsWith(PREFIX_EMAIL)) {
            return true;
        }

        Pattern p = Pattern.compile(REGEX_EMAIL, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            String str = m.group(0);
            return str.equals(text);
        }

        return false;
    }

    /*public static void openEmailDialog(final Context context, String mEmail) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (mEmail.startsWith(PREFIX_EMAIL)) {
            mEmail = mEmail.substring(PREFIX_EMAIL.length());
        }
        final String email = mEmail;
        builder.setTitle(email);
        builder.setItems(R.array.im_email_operation_arrays,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                // 默认Email帐号发送
                                AndroidIntentUtils.sendEmail(context, email);
                                break;
                            case 1:
                                // 复制
                                TxtUtils.copy(email);
                                break;
                        }
                    }
                });
        builder.create().show();
    }*/

  /*  public static void openEmailStraight(final Context context, String mEmail) {
        if (mEmail.startsWith(PREFIX_EMAIL)) {
            mEmail = mEmail.substring(PREFIX_EMAIL.length());
        }
        AndroidIntentUtils.sendEmail(context, mEmail);
    }*/

   /* public static void openPhoneDialog(final Context context, String mPhone) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (mPhone.startsWith(PREFIX_PHONE)) {
            mPhone = mPhone.substring(PREFIX_PHONE.length());
        }
        final String phone = mPhone;
        builder.setTitle(phone + context.getString(R.string.phone_call));
        builder.setItems(R.array.im_phone_operation_arrays,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                // 呼叫
                                AndroidIntentUtils.call(context, phone);
                                break;
                            case 1:
                                // 复制
                                TxtUtils.copy(phone);
                                break;
                            case 2:
                                // 添加到手机通讯录
                                openPhoneBookDialog(context, phone);
                                break;
                        }
                    }
                });
        builder.create().show();
    }*/

   /* public static void openPhoneStraight(final Context context, String mPhone) {
        if (mPhone.startsWith(PREFIX_PHONE)) {
            mPhone = mPhone.substring(PREFIX_PHONE.length());
        }
        AndroidIntentUtils.call(context, mPhone);
    }
*/
   /* public static void openPhoneBookDialog(final Context context, String mPhone) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (mPhone.startsWith(PREFIX_PHONE)) {
            mPhone = mPhone.substring(PREFIX_PHONE.length());
        }
        final String phone = mPhone;
        builder.setTitle(phone + context.getString(R.string.phone_call));
        builder.setItems(R.array.im_phone_book_operation_arrays,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                // 创建新联系人
                                AndroidIntentUtils.createContact(context, phone);
                                break;
                            case 1:
                                // 添加到现在联系人
                                AndroidIntentUtils.updateContact(context, phone);
                                break;
                        }
                    }
                });
        builder.create().show();


    }*/
}
