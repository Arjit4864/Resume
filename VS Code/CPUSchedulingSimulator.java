import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

class Proc {
    int pid;
    int arrival;
    int burst;
    int priority;
    int remaining;
    int start = -1;
    int finish = 0;
    int waiting = 0;
    int turnaround = 0;
    int response = -1;

    Proc(int pid, int arrival, int burst, int priority) {
        this.pid = pid;
        this.arrival = arrival;
        this.burst = burst;
        this.priority = priority;
        this.remaining = burst;
    }

    Proc copy() {
        Proc p = new Proc(pid, arrival, burst, priority);
        p.remaining = remaining;
        p.start = start;
        p.finish = finish;
        p.waiting = waiting;
        p.turnaround = turnaround;
        p.response = response;
        return p;
    }
}

class GanttEntry {
    String pidStr;
    int start;
    int end;

    GanttEntry(String pidStr, int start, int end) {
        this.pidStr = pidStr;
        this.start = start;
        this.end = end;
    }
}

public class CPUSchedulingSimulator extends JFrame {
    private JTable processTable;
    private DefaultTableModel processModel;
    private JTable resultTable;
    private DefaultTableModel resultModel;
    private JTextArea logArea;
    private GanttPanel ganttPanel;
    private JComboBox<String> algoCombo;
    private JTextField quantumField;

    public CPUSchedulingSimulator() {
        setTitle("CPU Scheduling Simulator - Swing");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        root.add(top, BorderLayout.NORTH);

        processModel = new DefaultTableModel(new Object[]{"PID", "Arrival", "Burst", "Priority"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 0;
            }
        };
        processTable = new JTable(processModel);
        processTable.setRowHeight(22);
        JScrollPane procScroll = new JScrollPane(processTable);
        procScroll.setPreferredSize(new Dimension(600, 160));

        for (int i = 1; i <= 4; ++i) {
            processModel.addRow(new Object[]{i, 0, 1 + i, 0});
        }

        JPanel controls = new JPanel();
        controls.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));

        JButton addRow = new JButton("Add Process");
        JButton delRow = new JButton("Delete Selected");
        controls.add(addRow);
        controls.add(delRow);

        algoCombo = new JComboBox<>(new String[]{"FCFS (Non-preemptive)", "SJF (Non-preemptive)", "SRTF (Preemptive)", "Priority (Non-preemptive)", "Priority (Preemptive)", "Round Robin"});
        controls.add(new JLabel("Algorithm:"));
        controls.add(algoCombo);

        controls.add(new JLabel("Quantum:"));
        quantumField = new JTextField("2", 4);
        controls.add(quantumField);

        JButton runBtn = new JButton("Run");
        controls.add(runBtn);

        top.add(procScroll, BorderLayout.CENTER);
        top.add(controls, BorderLayout.SOUTH);

        JPanel middle = new JPanel(new GridLayout(1, 2, 10, 10));
        root.add(middle, BorderLayout.CENTER);

        resultModel = new DefaultTableModel(new Object[]{"PID", "Arrival", "Burst", "Priority", "Start", "Finish", "Waiting", "Turnaround", "Response"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        resultTable = new JTable(resultModel);
        JScrollPane resScroll = new JScrollPane(resultTable);
        middle.add(resScroll);

        JPanel right = new JPanel(new BorderLayout(6,6));
        ganttPanel = new GanttPanel();
        ganttPanel.setPreferredSize(new Dimension(350, 300));
        right.add(ganttPanel, BorderLayout.CENTER);

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(350, 200));
        right.add(logScroll, BorderLayout.SOUTH);

        middle.add(right);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        root.add(bottom, BorderLayout.SOUTH);
        JLabel avgLabel = new JLabel("Averages: W = -, T = -, R = -");
        bottom.add(avgLabel);

        addRow.addActionListener(e -> {
            int pid = processModel.getRowCount() + 1;
            processModel.addRow(new Object[]{pid, 0, 1, 0});
        });
        delRow.addActionListener(e -> {
            int sel = processTable.getSelectedRow();
            if (sel >= 0) processModel.removeRow(sel);
        
            for (int i = 0; i < processModel.getRowCount(); ++i) processModel.setValueAt(i+1, i, 0);
        });

        runBtn.addActionListener(e -> {
            java.util.List<Proc> procs = readProcessesFromTable();
            String algo = (String) algoCombo.getSelectedItem();
            int quantum = 1;
            try { quantum = Math.max(1, Integer.parseInt(quantumField.getText())); } catch (Exception ex) { quantum = 1; }

            SimulatorResult res = runAlgorithm(procs, algo, quantum);
            populateResultTable(res.procs);
            ganttPanel.setGantt(res.gantt);
            ganttPanel.repaint();
            logArea.setText("");
            for (String s : res.waitHistory) logArea.append(s + "\n");
            avgLabel.setText(String.format("Averages: W = %.2f, T = %.2f, R = %.2f", res.avgWaiting, res.avgTurnaround, res.avgResponse));
        });
    }

    private java.util.List<Proc> readProcessesFromTable() {
        java.util.List<Proc> list = new ArrayList<>();
        for (int i = 0; i < processModel.getRowCount(); ++i) {
            int pid = i+1;
            int arrival = toInt(processModel.getValueAt(i,1));
            int burst = toInt(processModel.getValueAt(i,2));
            int pr = toInt(processModel.getValueAt(i,3));
            if (burst <= 0) burst = 1;
            list.add(new Proc(pid, arrival, burst, pr));
        }
        return list;
    }

    private int toInt(Object o) {
        try { return Integer.parseInt(String.valueOf(o).trim()); } catch(Exception e) { return 0; }
    }

    private void populateResultTable(java.util.List<Proc> procs) {
        resultModel.setRowCount(0);
        for (Proc p : procs) {
            resultModel.addRow(new Object[]{"P"+p.pid, p.arrival, p.burst, p.priority, p.start, p.finish, p.waiting, p.turnaround, p.response});
        }
    }

    static class SimulatorResult {
        java.util.List<Proc> procs;
        java.util.List<GanttEntry> gantt;
        java.util.List<String> waitHistory;
        double avgWaiting, avgTurnaround, avgResponse;

        SimulatorResult() {
            procs = new ArrayList<>();
            gantt = new ArrayList<>();
            waitHistory = new ArrayList<>();
        }
    }

    private SimulatorResult runAlgorithm(java.util.List<Proc> original, String algo, int quantum) {
    
        java.util.List<Proc> procs = new ArrayList<>();
        for (Proc p : original) procs.add(p.copy());

        SimulatorResult res = new SimulatorResult();
        switch (algo) {
            case "FCFS (Non-preemptive)":
                runFCFS(procs, res);
                break;
            case "SJF (Non-preemptive)":
                runSJFNonPreemptive(procs, res);
                break;
            case "SRTF (Preemptive)":
                runSRTF(procs, res);
                break;
            case "Priority (Non-preemptive)":
                runPriorityNonPreemptive(procs, res);
                break;
            case "Priority (Preemptive)":
                runPriorityPreemptive(procs, res);
                break;
            case "Round Robin":
                runRoundRobin(procs, res, quantum);
                break;
            default:
                runFCFS(procs, res);
        }

        double tw = 0, tt = 0, tr = 0;
        for (Proc p : procs) {
            tw += p.waiting; tt += p.turnaround; tr += p.response;
        }
        int n = procs.size();
        res.avgWaiting = tw / n;
        res.avgTurnaround = tt / n;
        res.avgResponse = tr / n;
        res.procs = procs;
        return res;
    }

    private void runFCFS(java.util.List<Proc> procs, SimulatorResult res) {
        procs.sort(Comparator.comparingInt((Proc p) -> p.arrival).thenComparingInt(p -> p.pid));
        int n = procs.size();
        int currentTime = 0;
        for (int i = 0; i < n; ++i) {
            Proc p = procs.get(i);
            if (currentTime < p.arrival) {
                res.gantt.add(new GanttEntry("Idle", currentTime, p.arrival));
                currentTime = p.arrival;
            }
            res.waitHistory.add(getWaitingQueueFCFS(procs, currentTime, i));
            p.start = currentTime;
            if (p.response == -1) p.response = p.start - p.arrival;
            p.waiting = p.start - p.arrival;
            p.finish = p.start + p.burst;
            p.turnaround = p.finish - p.arrival;
            res.gantt.add(new GanttEntry("P"+p.pid, p.start, p.finish));
            currentTime = p.finish;
        }
    }

    private String getWaitingQueueFCFS(java.util.List<Proc> procs, int currentTime, int index) {
        StringBuilder sb = new StringBuilder();
        sb.append("Waiting Queue at time ").append(currentTime).append(": [");
        boolean first = true;
        for (int i = index; i < procs.size(); ++i) {
            if (procs.get(i).arrival <= currentTime) {
                if (!first) sb.append(", ");
                sb.append("P").append(procs.get(i).pid);
                first = false;
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private String getWaitingQueueGeneral(java.util.List<Proc> procs, int currentTime, int currentIdx, boolean[] completed) {
        StringBuilder sb = new StringBuilder();
        sb.append("Waiting Queue at time ").append(currentTime).append(": [");
        boolean first = true;
        for (int i = 0; i < procs.size(); ++i) {
            if (!completed[i] && procs.get(i).arrival <= currentTime && i != currentIdx) {
                if (!first) sb.append(", ");
                sb.append("P").append(procs.get(i).pid);
                first = false;
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private String getWaitingQueueGeneral(java.util.List<Proc> procs, int currentTime, int currentIdx, java.util.List<Boolean> completed) {
        StringBuilder sb = new StringBuilder();
        sb.append("Waiting Queue at time ").append(currentTime).append(": [");
        boolean first = true;
        for (int i = 0; i < procs.size(); ++i) {
            if (!completed.get(i) && procs.get(i).arrival <= currentTime && i != currentIdx) {
                if (!first) sb.append(", ");
                sb.append("P").append(procs.get(i).pid);
                first = false;
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private void runSJFNonPreemptive(java.util.List<Proc> procs, SimulatorResult res) {
        procs.sort(Comparator.comparingInt((Proc p) -> p.arrival).thenComparingInt(p -> p.burst).thenComparingInt(p -> p.pid));
        int n = procs.size();
        int completed = 0, currentTime = 0;
        boolean[] done = new boolean[n];

        while (completed < n) {
            int idx = -1, minBurst = Integer.MAX_VALUE;
            for (int i = 0; i < n; ++i) {
                if (!done[i] && procs.get(i).arrival <= currentTime) {
                    if (procs.get(i).burst < minBurst) { minBurst = procs.get(i).burst; idx = i; }
                }
            }
            if (idx == -1) { currentTime++; continue; }
            res.waitHistory.add(getWaitingQueueGeneral(procs, currentTime, idx, done));
            Proc p = procs.get(idx);
            p.start = currentTime;
            if (p.response == -1) p.response = p.start - p.arrival;
            p.waiting = p.start - p.arrival;
            p.finish = p.start + p.burst;
            p.turnaround = p.finish - p.arrival;
            res.gantt.add(new GanttEntry("P"+p.pid, p.start, p.finish));
            currentTime = p.finish;
            done[idx] = true;
            completed++;
        }
    }

    private void runSRTF(java.util.List<Proc> procs, SimulatorResult res) {
        procs.sort(Comparator.comparingInt((Proc p) -> p.arrival).thenComparingInt(p -> p.burst).thenComparingInt(p -> p.pid));
        int n = procs.size();
        for (Proc p : procs) p.remaining = p.burst;
        boolean[] done = new boolean[n];
        int completed = 0, currentTime = 0, prevIdx = -1;

        while (completed < n) {
            int idx = -1, minRem = Integer.MAX_VALUE;
            for (int i = 0; i < n; ++i) {
                if (!done[i] && procs.get(i).arrival <= currentTime) {
                    if (procs.get(i).remaining < minRem) { minRem = procs.get(i).remaining; idx = i; }
                }
            }
            if (idx == -1) {
                int nextArrival = Integer.MAX_VALUE;
                for (int i = 0; i < n; ++i) if (!done[i]) nextArrival = Math.min(nextArrival, procs.get(i).arrival);
                int endTime = (nextArrival == Integer.MAX_VALUE ? currentTime + 1 : nextArrival);
                res.gantt.add(new GanttEntry("Idle", currentTime, endTime));
                currentTime = endTime;
                prevIdx = -2;
                continue;
            }
            java.util.List<Boolean> doneList = new ArrayList<>(); for (boolean b : done) doneList.add(b);
            res.waitHistory.add(getWaitingQueueGeneral(procs, currentTime, idx, doneList));
            Proc p = procs.get(idx);
            if (p.start == -1) p.start = currentTime;
            if (prevIdx != idx) {
                res.gantt.add(new GanttEntry("P"+p.pid, currentTime, currentTime+1));
            } else {
                GanttEntry last = res.gantt.get(res.gantt.size()-1);
                if (last.pidStr.equals("P"+p.pid) && last.end == currentTime) last.end = currentTime+1;
                else res.gantt.add(new GanttEntry("P"+p.pid, currentTime, currentTime+1));
            }
            p.remaining--;
            currentTime++;
            if (p.remaining == 0) {
                p.finish = currentTime;
                p.turnaround = p.finish - p.arrival;
                p.waiting = p.turnaround - p.burst;
                if (p.response == -1) p.response = p.start - p.arrival;
                done[idx] = true; completed++;
            }
            prevIdx = idx;
        }
    }

    private void runPriorityNonPreemptive(java.util.List<Proc> procs, SimulatorResult res) {
        procs.sort(Comparator.comparingInt((Proc p) -> p.arrival).thenComparingInt(p -> p.priority).thenComparingInt(p -> p.pid));
        int n = procs.size();
        int completed = 0, currentTime = 0;
        boolean[] done = new boolean[n];
        while (completed < n) {
            int idx = -1, bestPr = Integer.MAX_VALUE;
            for (int i = 0; i < n; ++i) {
                if (!done[i] && procs.get(i).arrival <= currentTime) {
                    if (procs.get(i).priority < bestPr) { bestPr = procs.get(i).priority; idx = i; }
                }
            }
            if (idx == -1) { currentTime++; continue; }
            res.waitHistory.add(getWaitingQueueGeneral(procs, currentTime, idx, done));
            Proc p = procs.get(idx);
            p.start = currentTime;
            if (p.response == -1) p.response = p.start - p.arrival;
            p.waiting = p.start - p.arrival;
            p.finish = p.start + p.burst;
            p.turnaround = p.finish - p.arrival;
            res.gantt.add(new GanttEntry("P"+p.pid, p.start, p.finish));
            currentTime = p.finish; done[idx] = true; completed++;
        }
    }

    private void runPriorityPreemptive(java.util.List<Proc> procs, SimulatorResult res) {
        procs.sort(Comparator.comparingInt((Proc p) -> p.arrival).thenComparingInt(p -> p.priority).thenComparingInt(p -> p.pid));
        int n = procs.size();
        for (Proc p : procs) p.remaining = p.burst;
        boolean[] done = new boolean[n];
        int completed = 0, currentTime = 0, prevIdx = -1;

        while (completed < n) {
            int idx = -1, bestPr = Integer.MAX_VALUE;
            for (int i = 0; i < n; ++i) {
                if (!done[i] && procs.get(i).arrival <= currentTime) {
                    if (procs.get(i).priority < bestPr) { bestPr = procs.get(i).priority; idx = i; }
                }
            }
            if (idx == -1) {
                int nextArrival = Integer.MAX_VALUE;
                for (int i = 0; i < n; ++i) if (!done[i]) nextArrival = Math.min(nextArrival, procs.get(i).arrival);
                int endTime = (nextArrival == Integer.MAX_VALUE ? currentTime + 1 : nextArrival);
                res.gantt.add(new GanttEntry("Idle", currentTime, endTime));
                currentTime = endTime; prevIdx = -2; continue;
            }
            java.util.List<Boolean> doneList = new ArrayList<>(); for (boolean b : done) doneList.add(b);
            res.waitHistory.add(getWaitingQueueGeneral(procs, currentTime, idx, doneList));
            Proc p = procs.get(idx);
            if (p.start == -1) p.start = currentTime;
            if (prevIdx != idx) res.gantt.add(new GanttEntry("P"+p.pid, currentTime, currentTime+1));
            else {
                GanttEntry last = res.gantt.get(res.gantt.size()-1);
                if (last.pidStr.equals("P"+p.pid) && last.end == currentTime) last.end = currentTime+1;
                else res.gantt.add(new GanttEntry("P"+p.pid, currentTime, currentTime+1));
            }
            p.remaining--;
            currentTime++;
            if (p.remaining == 0) {
                p.finish = currentTime;
                p.turnaround = p.finish - p.arrival;
                p.waiting = p.turnaround - p.burst;
                if (p.response == -1) p.response = p.start - p.arrival;
                done[idx] = true; completed++;
            }
            prevIdx = idx;
        }
    }

    private void runRoundRobin(java.util.List<Proc> procs, SimulatorResult res, int quantum) {
        procs.sort(Comparator.comparingInt((Proc p) -> p.arrival).thenComparingInt(p -> p.pid));
        int n = procs.size();
        for (Proc p : procs) p.remaining = p.burst;
        Queue<Integer> q = new ArrayDeque<>();
        boolean[] inQueue = new boolean[n], finished = new boolean[n];
        int currentTime = 0, idx = 0, completed = 0;
        while (idx < n && procs.get(idx).arrival <= currentTime) { q.add(idx); inQueue[idx] = true; idx++; }
        if (q.isEmpty() && idx < n) { currentTime = procs.get(idx).arrival; q.add(idx); inQueue[idx] = true; idx++; }
        while (completed < n) {
            if (q.isEmpty()) {
                if (idx < n) { currentTime = Math.max(currentTime, procs.get(idx).arrival); q.add(idx); inQueue[idx] = true; idx++; continue; }
                else break;
            }
            int i = q.poll(); inQueue[i] = false;
            Proc p = procs.get(i);
            if (p.start == -1) p.start = currentTime;
            java.util.List<Boolean> finishedList = new ArrayList<>(); for (boolean b : finished) finishedList.add(b);
            res.waitHistory.add(getWaitingQueueGeneral(procs, currentTime, i, finishedList));
            int exec = Math.min(quantum, p.remaining);
            
            if (!res.gantt.isEmpty() && res.gantt.get(res.gantt.size()-1).pidStr.equals("P"+p.pid) && res.gantt.get(res.gantt.size()-1).end == currentTime) {
                res.gantt.get(res.gantt.size()-1).end += exec;
            } else {
                res.gantt.add(new GanttEntry("P"+p.pid, currentTime, currentTime + exec));
            }
            currentTime += exec;
            p.remaining -= exec;
            while (idx < n && procs.get(idx).arrival <= currentTime) {
                if (!inQueue[idx] && !finished[idx] && procs.get(idx).remaining > 0) { q.add(idx); inQueue[idx] = true; }
                idx++;
            }
            if (p.remaining > 0) { q.add(i); inQueue[i] = true; }
            else {
                p.finish = currentTime;
                p.turnaround = p.finish - p.arrival;
                p.waiting = p.turnaround - p.burst;
                if (p.response == -1) p.response = p.start - p.arrival;
                finished[i] = true; completed++;
            }
        }
    }

    static class GanttPanel extends JPanel {
        java.util.List<GanttEntry> gantt = new ArrayList<>();

        void setGantt(java.util.List<GanttEntry> g) {
        
            gantt.clear();
            for (GanttEntry e : g) {
                if (!gantt.isEmpty()) {
                    GanttEntry last = gantt.get(gantt.size()-1);
                    if (last.pidStr.equals(e.pidStr) && last.end == e.start) last.end = e.end;
                    else gantt.add(new GanttEntry(e.pidStr, e.start, e.end));
                } else gantt.add(new GanttEntry(e.pidStr, e.start, e.end));
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(Color.WHITE);
            g2.fillRect(0,0,w,h);

            if (gantt.isEmpty()) {
                g2.setColor(Color.GRAY);
                g2.drawString("Gantt chart empty", 10, 20);
                return;
            }

            int minT = Integer.MAX_VALUE, maxT = Integer.MIN_VALUE;
            for (GanttEntry e : gantt) { minT = Math.min(minT, e.start); maxT = Math.max(maxT, e.end); }
            int total = Math.max(1, maxT - minT);
            int margin = 20;
            int barY = 30;
            int barHeight = 40;
            int availW = w - 2*margin;

            
            for (GanttEntry e : gantt) {
                int x1 = margin + (int) Math.round(((double)(e.start - minT) / total) * availW);
                int x2 = margin + (int) Math.round(((double)(e.end - minT) / total) * availW);
                int width = Math.max(3, x2 - x1);
                Color c = colorForLabel(e.pidStr);
                g2.setColor(c);
                g2.fillRect(x1, barY, width, barHeight);
                g2.setColor(Color.BLACK);
                g2.drawRect(x1, barY, width, barHeight);
            
                String label = e.pidStr;
                FontMetrics fm = g2.getFontMetrics();
                int strW = fm.stringWidth(label);
                int strX = x1 + Math.max(2, (width - strW)/2);
                int strY = barY + (barHeight + fm.getAscent())/2 - 4;
                g2.drawString(label, strX, strY);
            
                g2.drawString(String.valueOf(e.start), x1, barY + barHeight + 15);
            }
            
            GanttEntry last = gantt.get(gantt.size()-1);
            int lastX = margin + (int) Math.round(((double)(last.end - minT) / total) * availW);
            g2.drawString(String.valueOf(last.end), lastX, barY + barHeight + 15);
        }

        private Color colorForLabel(String s) {
            if (s.equalsIgnoreCase("Idle")) return Color.LIGHT_GRAY;
            int h = Math.abs(s.hashCode());
            float hue = (h % 360) / 360f;
            return Color.getHSBColor(hue, 0.6f, 0.9f);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CPUSchedulingSimulator app = new CPUSchedulingSimulator();
            app.setVisible(true);
        });
    }
}