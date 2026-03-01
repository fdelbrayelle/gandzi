<script setup lang="ts">
import { Bar } from 'vue-chartjs';
import { Chart, BarElement, CategoryScale, LinearScale, Tooltip, Legend } from 'chart.js';

Chart.register(BarElement, CategoryScale, LinearScale, Tooltip, Legend);

const props = defineProps<{
  labels: string[];
  values: number[];
  label: string;
}>();

const chartData = {
  labels: props.labels,
  datasets: [{
    label: props.label,
    data: props.values,
    backgroundColor: '#2f855a'
  }],
};
</script>

<template>
  <Bar :data="chartData" />
</template>
