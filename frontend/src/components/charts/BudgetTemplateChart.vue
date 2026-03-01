<script setup lang="ts">
import { Line } from 'vue-chartjs';
import { Chart, LineElement, CategoryScale, LinearScale, PointElement, Tooltip, Legend } from 'chart.js';

Chart.register(LineElement, CategoryScale, LinearScale, PointElement, Tooltip, Legend);

const props = defineProps<{
  labels: string[];
  planned: number[];
}>();

const chartData = {
  labels: props.labels,
  datasets: [{
    label: 'Monthly Budget Template',
    data: props.planned,
    borderColor: '#1a365d',
    backgroundColor: 'rgba(26,54,93,0.2)'
  }],
};
</script>

<template>
  <Line :data="chartData" />
</template>
