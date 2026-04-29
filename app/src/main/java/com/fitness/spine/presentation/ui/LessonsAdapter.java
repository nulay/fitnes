package com.fitness.spine.presentation.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.fitness.spine.R;
import com.fitness.spine.data.model.LessonInfo;

import java.util.ArrayList;
import java.util.List;

public class LessonsAdapter extends BaseAdapter {

    private final Context context;
    private final List<LessonInfo> lessons;

    public LessonsAdapter(Context context, List<LessonInfo> lessons) {
        this.context = context;
        this.lessons = lessons != null ? lessons : new ArrayList<>();
    }

    @Override
    public int getCount() {
        return lessons.size();
    }

    @Override
    public Object getItem(int position) {
        return lessons.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_lesson, parent, false);
            holder = new ViewHolder();
            holder.tvTitle = convertView.findViewById(R.id.tvLessonTitle);
            holder.tvDescription = convertView.findViewById(R.id.tvLessonDescription);
            holder.tvInfo = convertView.findViewById(R.id.tvLessonInfo);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        LessonInfo lesson = lessons.get(position);
        holder.tvTitle.setText(lesson.getTitle());
        holder.tvDescription.setText(lesson.getDescription());

        String info = lesson.getDifficultyDisplay() + " • " + lesson.getDurationMinutes() + " мин";
        holder.tvInfo.setText(info);

        return convertView;
    }

    private static class ViewHolder {
        TextView tvTitle;
        TextView tvDescription;
        TextView tvInfo;
    }
}