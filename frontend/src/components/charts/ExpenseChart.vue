<script setup lang="ts">
import { Pie } from 'vue-chartjs';
import { Chart, ArcElement, Tooltip, Legend } from 'chart.js';

Chart.register(ArcElement, Tooltip, Legend);

const props = defineProps<{
  labels: string[];
  totals: number[];
}>();

const chartData = {
  labels: props.labels,
  datasets: [{
    label: 'Effective Expenses',
    data: props.totals,
    backgroundColor: ['#2b6cb0', '#2f855a', '#d69e2e', '#c53030', '#4a5568']
  }],
};
</script>

<template>
  <Pie :data="chartData" />
</template>
