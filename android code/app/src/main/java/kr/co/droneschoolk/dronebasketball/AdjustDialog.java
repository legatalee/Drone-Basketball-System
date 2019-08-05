package kr.co.droneschoolk.dronebasketball;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.github.zagum.switchicon.SwitchIconView;
import com.xw.repo.BubbleSeekBar;

public class AdjustDialog {

    private DialogListener dialogListener;
    private Context context;

    public AdjustDialog(Context context) {
        this.context = context;
    }

    public void setDialogListener(DialogListener dialogListener) {
        this.dialogListener = dialogListener;
    }

    // 호출할 다이얼로그 함수를 정의한다.
    public void callFunction(final int main_label) {

        // 커스텀 다이얼로그를 정의하기위해 Dialog클래스를 생성한다.
        final Dialog dlg = new Dialog(context);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();

        Window window = dlg.getWindow();
        lp.copyFrom(window.getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        // 액티비티의 타이틀바를 숨긴다.
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dlg.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 커스텀 다이얼로그의 레이아웃을 설정한다.
        dlg.setContentView(R.layout.adjust_dialog);
        dlg.show();
        window.setAttributes(lp);

        final View resetButton = (View) dlg.findViewById(R.id.reset_button);
        final SwitchIconView resetToggle = (SwitchIconView) dlg.findViewById(R.id.reset_toggle);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity) MainActivity.mContext).reset();
                Toast.makeText(context, "충격량이 초기화되었습니다.", Toast.LENGTH_SHORT).show();
                dlg.dismiss();
            }
        });

        // 커스텀 다이얼로그의 각 위젯들을 정의한다.
        final Button confirmButton = (Button) dlg.findViewById(R.id.confirm);
        final BubbleSeekBar mBubbleSeekBar = (BubbleSeekBar) dlg.findViewById(R.id.bubble_seekbar);

        mBubbleSeekBar.getConfigBuilder()
                .min(500)
                .max(5000)
                .progress(20)
                .sectionCount(9)
                .showSectionText()
                .sectionTextSize(18)
                .showThumbText()
                .thumbTextSize(18)
                .bubbleTextSize(18)
                .showSectionMark()
                .seekBySection()
                .autoAdjustSectionMark()
                .sectionTextPosition(BubbleSeekBar.TextPosition.BELOW_SECTION_MARK)
                .build();

        mBubbleSeekBar.setProgress(main_label);

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(context, "충격량 최댓값이 " + mBubbleSeekBar.getProgress() + "으로 설정되었습니다.", Toast.LENGTH_SHORT).show();
                dialogListener.onPositiveClicked(mBubbleSeekBar.getProgress()); // 인터페이스의 메소드를 호출
                dlg.dismiss();
            }
        });
    }

    public interface DialogListener {
        public void onPositiveClicked(int progress);
    }
}